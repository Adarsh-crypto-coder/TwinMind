package com.example.twinmind;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TranscriptionManager {

    private static final String TAG = "GoogleSpeechTranscription";

    private static final String GOOGLE_SPEECH_API_URL =
            "https://speech.googleapis.com/v1/speech:recognize?key=";
    private static final String API_KEY = "AIzaSyATcaLbPkSCMa0S3YiIFocBZh5biRuHuko";

    private static final long MIN_REQUEST_INTERVAL_MS = 1000;
    private long lastRequestTime = 0;

    private final Context context;
    private final OkHttpClient httpClient;
    private final ExecutorService transcriptionExecutor;
    private final TranscriptionDatabaseHelper dbHelper;
    private final Handler mainHandler;

    private TranscriptionListener listener;
    private String currentSessionId;

    public interface TranscriptionListener {
        void onTranscriptionReceived(String transcription, long timestamp);
        void onTranscriptionError(String error);
    }

    public TranscriptionManager(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.transcriptionExecutor = Executors.newSingleThreadExecutor();
        this.dbHelper = TranscriptionDatabaseHelper.getInstance(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setTranscriptionListener(TranscriptionListener listener) {
        this.listener = listener;
    }

    public void setCurrentSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    public void transcribeAudioChunk(byte[] audioData) {
        long timestamp = System.currentTimeMillis();

        transcriptionExecutor.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRequest = currentTime - lastRequestTime;

                if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                    long delayNeeded = MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest;
                    Thread.sleep(delayNeeded);
                }
                lastRequestTime = System.currentTimeMillis();
                byte[] wavData = convertToWav(audioData);
                transcribeWithGoogleSpeech(wavData, timestamp);

            } catch (Exception e) {
                Log.e(TAG, "Error processing audio chunk", e);
                if (listener != null) {
                    mainHandler.post(() -> listener.onTranscriptionError("Error processing audio: " + e.getMessage()));
                }
            }
        });
    }

    private void transcribeWithGoogleSpeech(byte[] audioData, long timestamp) {
        try {
            String encodedAudio = Base64.encodeToString(audioData, Base64.NO_WRAP);
            JSONObject request = new JSONObject();
            JSONObject config = new JSONObject();
            JSONObject audio = new JSONObject();
            config.put("encoding", "LINEAR16");
            config.put("sampleRateHertz", 16000);
            config.put("languageCode", "en-US");
            config.put("enableAutomaticPunctuation", true);
            config.put("model", "latest_short");
            audio.put("content", encodedAudio);

            request.put("config", config);
            request.put("audio", audio);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    request.toString()
            );

            Request httpRequest = new Request.Builder()
                    .url(GOOGLE_SPEECH_API_URL + API_KEY)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Sending transcription request to Google Speech API");

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Google Speech API call failed", e);
                    if (listener != null) {
                        mainHandler.post(() -> listener.onTranscriptionError("Network error: " + e.getMessage()));
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, "Google Speech API response: " + responseBody);

                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if (jsonResponse.has("results") &&
                                    jsonResponse.getJSONArray("results").length() > 0) {

                                JSONObject result = jsonResponse.getJSONArray("results").getJSONObject(0);
                                if (result.has("alternatives") &&
                                        result.getJSONArray("alternatives").length() > 0) {

                                    JSONObject alternative = result.getJSONArray("alternatives").getJSONObject(0);
                                    String transcription = alternative.getString("transcript").trim();

                                    if (!transcription.isEmpty()) {
                                        Log.d(TAG, "Transcription successful: " + transcription);

                                        saveTranscriptionToDatabase(transcription, timestamp);

                                        if (listener != null) {
                                            mainHandler.post(() -> listener.onTranscriptionReceived(transcription, timestamp));
                                        }
                                    } else {
                                        Log.d(TAG, "Empty transcription result");
                                    }
                                } else {
                                    Log.d(TAG, "No alternatives in response");
                                }
                            } else {
                                Log.d(TAG, "No results in response - possibly silence or unclear audio");
                            }

                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing Google Speech response", e);
                            if (listener != null) {
                                mainHandler.post(() -> listener.onTranscriptionError("Response parsing error"));
                            }
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Google Speech API error " + response.code() + ": " + errorBody);

                        if (listener != null) {
                            mainHandler.post(() -> listener.onTranscriptionError("API error: " + response.code()));
                        }
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating request JSON", e);
            if (listener != null) {
                mainHandler.post(() -> listener.onTranscriptionError("Request creation error"));
            }
        }
    }

    private byte[] convertToWav(byte[] rawAudio) {
        int totalDataLen = rawAudio.length + 36;
        int totalFileLen = totalDataLen - 8;
        int sampleRate = 16000;
        int channels = 1;
        int byteRate = sampleRate * channels * 2;

        byte[] header = new byte[44];

        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte) (totalFileLen & 0xff);
        header[5] = (byte) ((totalFileLen >> 8) & 0xff);
        header[6] = (byte) ((totalFileLen >> 16) & 0xff);
        header[7] = (byte) ((totalFileLen >> 24) & 0xff);

        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;

        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        header[32] = (byte) (channels * 2); header[33] = 0;
        header[34] = 16; header[35] = 0;

        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (rawAudio.length & 0xff);
        header[41] = (byte) ((rawAudio.length >> 8) & 0xff);
        header[42] = (byte) ((rawAudio.length >> 16) & 0xff);
        header[43] = (byte) ((rawAudio.length >> 24) & 0xff);

        byte[] wavFile = new byte[header.length + rawAudio.length];
        System.arraycopy(header, 0, wavFile, 0, header.length);
        System.arraycopy(rawAudio, 0, wavFile, header.length, rawAudio.length);

        return wavFile;
    }

    private void saveTranscriptionToDatabase(String transcription, long timestamp) {
        if (currentSessionId != null) {
            int chunkIndex = dbHelper.getTranscriptionsForSession(currentSessionId).size();
            dbHelper.insertTranscription(currentSessionId, transcription, timestamp, chunkIndex);
            Log.d(TAG, "Saved transcription to database for session: " + currentSessionId);
        } else {
            Log.w(TAG, "No current session ID set, cannot save transcription");
        }
    }

    public void syncPendingTranscriptions() {
        Log.d(TAG, "Sync pending transcriptions called - not applicable for Google Speech API");
    }

    public void cleanup() {
        if (transcriptionExecutor != null) {
            transcriptionExecutor.shutdown();
        }
    }
}