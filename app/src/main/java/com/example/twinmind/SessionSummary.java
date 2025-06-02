package com.example.twinmind;

public class SessionSummary {
    public String sessionId;
    public long startTime;
    public long endTime;
    public long duration;
    public String firstTranscription;
    public int transcriptionCount;

    public SessionSummary(String sessionId, long startTime, long endTime,
                          String firstTranscription, int transcriptionCount) {
        this.sessionId = sessionId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = endTime - startTime;
        this.firstTranscription = firstTranscription;
        this.transcriptionCount = transcriptionCount;
    }
}