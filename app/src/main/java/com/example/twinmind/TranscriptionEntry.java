package com.example.twinmind;

public class TranscriptionEntry {
    public String sessionId;
    public String transcriptionText;
    public long timestamp;
    public int chunkIndex;

    public TranscriptionEntry() {}

    public TranscriptionEntry(String sessionId, String transcriptionText, long timestamp, int chunkIndex) {
        this.sessionId = sessionId;
        this.transcriptionText = transcriptionText;
        this.timestamp = timestamp;
        this.chunkIndex = chunkIndex;
    }
}