package com.example.twinmind;

public class RecordingSession {
    public String sessionId;
    public String title;
    public long startTime;
    public long endTime;
    public long duration;
    public String location;

    public RecordingSession() {}

    public RecordingSession(String sessionId, String title, long startTime, String location) {
        this.sessionId = sessionId;
        this.title = title;
        this.startTime = startTime;
        this.location = location;
    }
}