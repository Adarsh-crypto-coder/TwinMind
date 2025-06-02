package com.example.twinmind;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class AudioBufferManager {

    private static final String TAG = "AudioBufferManager";
    private static final int MAX_BUFFER_SIZE = 1024 * 1024; // 1MB max buffer

    private final Context context;
    private final ReentrantLock bufferLock;
    private ByteArrayOutputStream audioBuffer;
    private File tempAudioDir;

    public AudioBufferManager(Context context) {
        this.context = context;
        this.bufferLock = new ReentrantLock();
        this.audioBuffer = new ByteArrayOutputStream();
        initTempDirectory();
    }

    private void initTempDirectory() {
        tempAudioDir = new File(context.getCacheDir(), "temp_audio");
        if (!tempAudioDir.exists()) {
            tempAudioDir.mkdirs();
        }
    }

    public void addAudioData(byte[] data, int length) {
        bufferLock.lock();
        try {
            // Check if buffer is getting too large
            if (audioBuffer.size() + length > MAX_BUFFER_SIZE) {
                Log.w(TAG, "Audio buffer reaching max size, saving to temp file");
                saveBufferToTempFile();
                audioBuffer.reset();
            }

            audioBuffer.write(data, 0, length);
        } finally {
            bufferLock.unlock();
        }
    }

    public byte[] getAndClearBuffer() {
        bufferLock.lock();
        try {
            if (audioBuffer.size() == 0) {
                return null;
            }

            byte[] data = audioBuffer.toByteArray();
            audioBuffer.reset();
            return data;
        } finally {
            bufferLock.unlock();
        }
    }

    private void saveBufferToTempFile() {
        try {
            File tempFile = new File(tempAudioDir, "audio_chunk_" + System.currentTimeMillis() + ".raw");
            FileOutputStream fos = new FileOutputStream(tempFile);
            audioBuffer.writeTo(fos);
            fos.close();

            Log.d(TAG, "Saved audio buffer to temp file: " + tempFile.getName());
        } catch (IOException e) {
            Log.e(TAG, "Error saving buffer to temp file", e);
        }
    }

    public void clearTempFiles() {
        if (tempAudioDir != null && tempAudioDir.exists()) {
            File[] files = tempAudioDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        Log.d(TAG, "Deleted temp file: " + file.getName());
                    }
                }
            }
        }
    }

    public long getBufferSize() {
        bufferLock.lock();
        try {
            return audioBuffer.size();
        } finally {
            bufferLock.unlock();
        }
    }
}