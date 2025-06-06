package com.example.twinmind;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecordingActivity extends AppCompatActivity implements TranscriptionManager.TranscriptionListener {

    private static final String TAG = "RecordingActivity";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final int TRANSCRIPTION_INTERVAL_MS = 30000; // 30 seconds

    // UI Components
    private TextView tvTimer;
    private TextView tvStatus;
    private TextView tvLatestTranscription;
    private TextView tvDuration;
    private TextView tvTranscriptionCount;
    private TextView tvPauseResume;
    private ImageView ivPauseResume;
    private ImageView btnBack;
    private Button btnStop;
    private LinearLayout btnPauseResume;
    private LinearLayout transcriptionPreview;
    private LinearLayout audioVisualizer;

    // Audio Recording
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private ExecutorService recordingExecutor;
    private ScheduledExecutorService transcriptionScheduler;

    private TranscriptionManager transcriptionManager;
    private AudioBufferManager audioBufferManager;
    private TranscriptionDatabaseHelper dbHelper;

    private String currentSessionId;
    private int transcriptionChunkIndex = 0;
    private boolean permissionsAlreadyGranted = false;

    // Timer
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long recordingStartTime;
    private long totalPausedTime = 0;
    private long pauseStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recording);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main1), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initManagers();
        setupClickListeners();

        permissionsAlreadyGranted = getIntent().getBooleanExtra("PERMISSIONS_GRANTED", false);

        if (permissionsAlreadyGranted || checkAllPermissions()) {
            startRecording();
        } else {
            Log.e(TAG, "Permissions not granted - this shouldn't happen");
            Toast.makeText(this, "Recording permissions not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        tvTimer = findViewById(R.id.tv_timer);
        tvStatus = findViewById(R.id.tv_status);
        tvLatestTranscription = findViewById(R.id.tv_latest_transcription);
        tvDuration = findViewById(R.id.tv_duration);
        tvTranscriptionCount = findViewById(R.id.tv_transcription_count);
        tvPauseResume = findViewById(R.id.tv_pause_resume);
        ivPauseResume = findViewById(R.id.iv_pause_resume);
        btnBack = findViewById(R.id.btn_back);
        btnStop = findViewById(R.id.btn_stop);
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        transcriptionPreview = findViewById(R.id.transcription_preview);
        audioVisualizer = findViewById(R.id.audio_visualizer);
    }

    private void initManagers() {
        currentSessionId = "session_" + System.currentTimeMillis();
        transcriptionManager = new TranscriptionManager(this);
        transcriptionManager.setTranscriptionListener(this);
        transcriptionManager.setCurrentSessionId(currentSessionId);

        audioBufferManager = new AudioBufferManager(this);
        dbHelper = TranscriptionDatabaseHelper.getInstance(this);
        recordingExecutor = Executors.newSingleThreadExecutor();
        transcriptionScheduler = Executors.newScheduledThreadPool(2);
        timerHandler = new Handler(Looper.getMainLooper());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            stopRecording();
            finish();
        });

        btnStop.setOnClickListener(v -> {
            stopRecording();
            goToSummaryScreen();
        });

        btnPauseResume.setOnClickListener(v -> {
            if (isRecording && !isPaused) {
                pauseRecording();
            } else if (isPaused) {
                resumeRecording();
            }
        });
    }

    private boolean checkAllPermissions() {
        boolean micPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        boolean storagePermission = true;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            storagePermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        return micPermission && storagePermission;
    }

    private void startRecording() {
        if (isRecording) {
            Log.w(TAG, "Recording already in progress");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted");
            Toast.makeText(this, "Microphone permission not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: " + bufferSize);
                Toast.makeText(this, "Audio configuration not supported", Toast.LENGTH_LONG).show();
                return;
            }

            bufferSize *= BUFFER_SIZE_FACTOR;

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed - state: " + audioRecord.getState());
                Toast.makeText(this, "Failed to initialize audio recording", Toast.LENGTH_LONG).show();
                if (audioRecord != null) {
                    audioRecord.release();
                    audioRecord = null;
                }
                return;
            }

            audioRecord.startRecording();

            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "AudioRecord failed to start recording - state: " + audioRecord.getRecordingState());
                Toast.makeText(this, "Failed to start audio recording", Toast.LENGTH_LONG).show();
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                return;
            }

            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            totalPausedTime = 0;

            Log.d(TAG, "Recording started successfully");

            dbHelper.createRecordingSession(
                    currentSessionId,
                    "Recording Session",
                    recordingStartTime,
                    "Current Location"
            );

            updateUI();
            startTimer();
            startAudioCapture();
            startPeriodicTranscription();

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when starting recording", e);
            Toast.makeText(this, "Permission denied for audio recording", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            Toast.makeText(this, "Failed to start recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void pauseRecording() {
        if (!isRecording || isPaused) return;

        Log.d(TAG, "Pausing recording...");
        isPaused = true;
        pauseStartTime = System.currentTimeMillis();

        if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            try {
                audioRecord.stop();
                Log.d(TAG, "AudioRecord paused");
            } catch (Exception e) {
                Log.e(TAG, "Error pausing audio record", e);
            }
        }
        byte[] pauseChunk = audioBufferManager.getAndClearBuffer();
        if (pauseChunk != null && pauseChunk.length > 0) {
            transcriptionManager.transcribeAudioChunk(pauseChunk);
        }

        updateUI();
        updatePauseResumeButton();
        Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show();
    }

    private void resumeRecording() {
        if (!isRecording || !isPaused) return;

        Log.d(TAG, "Resuming recording...");

        if (pauseStartTime > 0) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime;
            pauseStartTime = 0;
        }

        try {
            if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();

                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    isPaused = false;
                    Log.d(TAG, "AudioRecord resumed");

                    if (recordingExecutor.isShutdown()) {
                        recordingExecutor = Executors.newSingleThreadExecutor();
                        startAudioCapture();
                    }

                    updateUI();
                    updatePauseResumeButton();
                    Toast.makeText(this, "Recording resumed", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to resume recording");
                    Toast.makeText(this, "Failed to resume recording", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming recording", e);
            Toast.makeText(this, "Error resuming recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePauseResumeButton() {
        runOnUiThread(() -> {
            if (isPaused) {
                tvPauseResume.setText("Resume");
            } else {
                tvPauseResume.setText("Pause");
            }
        });
    }

    private void startAudioCapture() {
        recordingExecutor.execute(() -> {
            byte[] audioBuffer = new byte[1024];
            Log.d(TAG, "Audio capture thread started");

            while (isRecording && audioRecord != null) {
                if (isPaused) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                    continue;
                }

                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    try {
                        int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);

                        if (bytesRead > 0) {
                            audioBufferManager.addAudioData(audioBuffer, bytesRead);
                            Log.v(TAG, "Audio data captured: " + bytesRead + " bytes");
                        } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                            Log.e(TAG, "AudioRecord read error: ERROR_INVALID_OPERATION");
                            break;
                        } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                            Log.e(TAG, "AudioRecord read error: ERROR_BAD_VALUE");
                            break;
                        } else if (bytesRead == AudioRecord.ERROR_DEAD_OBJECT) {
                            Log.e(TAG, "AudioRecord read error: ERROR_DEAD_OBJECT");
                            break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading audio data", e);
                        break;
                    }
                }
            }

            Log.d(TAG, "Audio capture thread finished");
        });
    }

    private void startPeriodicTranscription() {
        transcriptionScheduler.scheduleAtFixedRate(() -> {
            if (isRecording && !isPaused) {
                byte[] audioChunk = audioBufferManager.getAndClearBuffer();
                if (audioChunk != null && audioChunk.length > 0) {
                    Log.d(TAG, "Sending audio chunk for transcription: " + audioChunk.length + " bytes");
                    transcriptionManager.transcribeAudioChunk(audioChunk);
                }
            }
        }, TRANSCRIPTION_INTERVAL_MS, TRANSCRIPTION_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime;

                    if (isPaused && pauseStartTime > 0) {
                        elapsedTime = (pauseStartTime - recordingStartTime) - totalPausedTime;
                    } else {
                        elapsedTime = (currentTime - recordingStartTime) - totalPausedTime;
                    }

                    updateTimerDisplay(elapsedTime);
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateTimerDisplay(long elapsedTime) {
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        tvTimer.setText(timeString);
        tvDuration.setText(timeString);
    }

    private void updateUI() {
        runOnUiThread(() -> {
            if (isRecording) {
                if (isPaused) {
                    tvStatus.setText("Recording paused");
                } else {
                    tvStatus.setText("TwinMind is listening and transcribing");
                }
            } else {
                tvStatus.setText("Recording stopped");
            }
        });
    }

    private void stopRecording() {
        if (!isRecording) return;

        Log.d(TAG, "Stopping recording...");
        isRecording = false;
        isPaused = false;
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - recordingStartTime - totalPausedTime;

        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
                Log.d(TAG, "AudioRecord stopped and released");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping audio record", e);
            }
            audioRecord = null;
        }
        byte[] finalChunk = audioBufferManager.getAndClearBuffer();
        if (finalChunk != null && finalChunk.length > 0) {
            transcriptionManager.transcribeAudioChunk(finalChunk);
        }

        dbHelper.endRecordingSession(currentSessionId, endTime, totalDuration);

        if (recordingExecutor != null && !recordingExecutor.isShutdown()) {
            recordingExecutor.shutdown();
        }
        if (transcriptionScheduler != null && !transcriptionScheduler.isShutdown()) {
            transcriptionScheduler.shutdown();
        }

        transcriptionManager.syncPendingTranscriptions();

        audioBufferManager.clearTempFiles();

        updateUI();
        Log.d(TAG, "Recording stopped successfully");
    }

    private void goToSummaryScreen() {
        Intent intent = new Intent(this, RecordingSummaryActivity.class);
        intent.putExtra("SESSION_ID", currentSessionId);
        intent.putExtra("RECORDING_DURATION", tvDuration.getText().toString());
        intent.putExtra("TRANSCRIPTION_COUNT", transcriptionChunkIndex);
        startActivity(intent);
        finish();
    }

    @Override
    public void onTranscriptionReceived(String transcription, long timestamp) {
        runOnUiThread(() -> {
            dbHelper.insertTranscription(currentSessionId, transcription, timestamp, transcriptionChunkIndex++);
            showLatestTranscription(transcription);
            updateTranscriptionCount();
            Toast.makeText(this, "Transcription received", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Transcription: " + transcription);
        });
    }

    @Override
    public void onTranscriptionError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Transcription error: " + error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Transcription error: " + error);
        });
    }

    private void showLatestTranscription(String transcription) {
        if (transcription != null && !transcription.trim().isEmpty()) {
            tvLatestTranscription.setText(transcription);
            transcriptionPreview.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void updateTranscriptionCount() {
        tvTranscriptionCount.setText(String.valueOf(transcriptionChunkIndex));
    }

    private void addTestTranscriptionData() {
        Log.d(TAG, "Adding test transcription data for testing...");

        // Add realistic test transcriptions with proper timing
        long baseTime = System.currentTimeMillis();

        dbHelper.insertTranscription(currentSessionId,
                "Hello everyone, welcome to today's meeting. Let's start by reviewing our progress from last week and discussing the upcoming milestones.",
                baseTime - 240000, 0);

        dbHelper.insertTranscription(currentSessionId,
                "Our main objectives for this quarter are to increase user engagement by 25% and improve our conversion rates. Sarah, can you share the latest analytics from the dashboard?",
                baseTime - 180000, 1);

        dbHelper.insertTranscription(currentSessionId,
                "Based on the data, we're seeing a 15% increase in daily active users compared to last month. However, our retention rate needs improvement, especially in the first week after signup.",
                baseTime - 120000, 2);

        dbHelper.insertTranscription(currentSessionId,
                "Let's schedule follow-up meetings with the product team to address the retention issues. John will handle the budget review by Friday, and we need to finalize the roadmap for Q2.",
                baseTime - 60000, 3);

        dbHelper.insertTranscription(currentSessionId,
                "Thank you everyone for your time today. Please send me your action items by end of day tomorrow, and we'll reconvene next Tuesday to review progress.",
                baseTime, 4);

        transcriptionChunkIndex = 5;
        updateTranscriptionCount();

        Log.d(TAG, "Test transcription data added successfully - total chunks: " + transcriptionChunkIndex);

        // Show a toast to indicate test data was added
        runOnUiThread(() -> {
            Toast.makeText(this, "Using test transcription data for demo", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();

        if (transcriptionManager != null) {
            transcriptionManager.cleanup();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        stopRecording();
        super.onBackPressed();
    }
}