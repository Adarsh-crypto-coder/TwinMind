package com.example.twinmind;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecordingService extends Service implements TranscriptionManager.TranscriptionListener {

    private static final String TAG = "RecordingService";
    private static final String CHANNEL_ID = "RecordingServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final int TRANSCRIPTION_INTERVAL_MS = 30000;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private ExecutorService recordingExecutor;
    private ScheduledExecutorService transcriptionScheduler;

    // Managers
    private TranscriptionManager transcriptionManager;
    private AudioBufferManager audioBufferManager;
    private TranscriptionDatabaseHelper dbHelper;

    private String currentSessionId;
    private int transcriptionChunkIndex = 0;
    private long recordingStartTime;
    private final IBinder binder = new RecordingBinder();

    public class RecordingBinder extends Binder {
        RecordingService getService() {
            return RecordingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        initializeComponents();

        Log.d(TAG, "Recording service created");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recording Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Background audio recording for TwinMind");
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void initializeComponents() {
        transcriptionManager = new TranscriptionManager(this);
        transcriptionManager.setTranscriptionListener(this);
        audioBufferManager = new AudioBufferManager(this);
        dbHelper = TranscriptionDatabaseHelper.getInstance(this);
        recordingExecutor = Executors.newSingleThreadExecutor();
        transcriptionScheduler = Executors.newScheduledThreadPool(2);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if ("START_RECORDING".equals(action)) {
            startRecording();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecording();
            stopSelf();
        }

        return START_STICKY;
    }

    private void startRecording() {
        if (isRecording) return;

        try {
            // Create unique session ID
            currentSessionId = "session_" + System.currentTimeMillis();
            recordingStartTime = System.currentTimeMillis();
            transcriptionChunkIndex = 0;

            // Initialize AudioRecord
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                    * BUFFER_SIZE_FACTOR;

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }

            audioRecord.startRecording();
            isRecording = true;

            dbHelper.createRecordingSession(
                    currentSessionId,
                    "Background Recording",
                    recordingStartTime,
                    "Background Location"
            );

            startForeground(NOTIFICATION_ID, createNotification());
            startAudioCapture();
            startPeriodicTranscription();

            Log.d(TAG, "Recording started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            stopRecording();
        }
    }

    private void startAudioCapture() {
        recordingExecutor.execute(() -> {
            byte[] audioBuffer = new byte[1024];

            while (isRecording && audioRecord != null) {
                try {
                    int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    if (bytesRead > 0) {
                        audioBufferManager.addAudioData(audioBuffer, bytesRead);
                    } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "AudioRecord read error: INVALID_OPERATION");
                        break;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error reading audio data", e);
                    break;
                }
            }
        });
    }

    private void startPeriodicTranscription() {
        transcriptionScheduler.scheduleAtFixedRate(() -> {
            if (isRecording) {
                byte[] audioChunk = audioBufferManager.getAndClearBuffer();
                if (audioChunk != null && audioChunk.length > 0) {
                    transcriptionManager.transcribeAudioChunk(audioChunk);
                }
            }
        }, TRANSCRIPTION_INTERVAL_MS, TRANSCRIPTION_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopRecording() {
        if (!isRecording) return;

        isRecording = false;
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - recordingStartTime;

        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record", e);
            }
            audioRecord = null;
        }
        byte[] finalChunk = audioBufferManager.getAndClearBuffer();
        if (finalChunk != null && finalChunk.length > 0) {
            transcriptionManager.transcribeAudioChunk(finalChunk);
        }

        if (currentSessionId != null) {
            dbHelper.endRecordingSession(currentSessionId, endTime, totalDuration);
        }

        if (recordingExecutor != null) {
            recordingExecutor.shutdown();
        }
        if (transcriptionScheduler != null) {
            transcriptionScheduler.shutdown();
        }

        audioBufferManager.clearTempFiles();

        Log.d(TAG, "Recording stopped successfully");
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, RecordingActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction("STOP_RECORDING");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TwinMind Recording")
                .setContentText("Recording audio in background")
                .setSmallIcon(R.drawable.ic_record_dot)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();

        if (transcriptionManager != null) {
            transcriptionManager.cleanup();
        }

        Log.d(TAG, "Recording service destroyed");
    }

    // TranscriptionListener
    @Override
    public void onTranscriptionReceived(String transcription, long timestamp) {
        // Save transcription to database
        if (currentSessionId != null) {
            dbHelper.insertTranscription(currentSessionId, transcription, timestamp, transcriptionChunkIndex++);
            Log.d(TAG, "Transcription saved: " + transcription.substring(0, Math.min(50, transcription.length())));
        }
        updateNotificationWithTranscription(transcription);
    }

    @Override
    public void onTranscriptionError(String error) {
        Log.e(TAG, "Transcription error in service: " + error);
    }

    private void updateNotificationWithTranscription(String transcription) {
        String snippet = transcription.length() > 30 ?
                transcription.substring(0, 30) + "..." : transcription;

        Notification updatedNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("TwinMind Recording")
                .setContentText("Latest: " + snippet)
                .setSmallIcon(R.drawable.ic_record_dot)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, updatedNotification);
    }

    public boolean isRecording() {
        return isRecording;
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public long getRecordingDuration() {
        if (isRecording && recordingStartTime > 0) {
            return System.currentTimeMillis() - recordingStartTime;
        }
        return 0;
    }
}