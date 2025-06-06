package com.example.twinmind;

public class ChatMessage {
    public enum MessageType {
        USER, ASSISTANT, SYSTEM
    }

    public String id;
    public String content;
    public MessageType type;
    public long timestamp;

    public ChatMessage() {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String content, MessageType type) {
        this();
        this.content = content;
        this.type = type;
    }
}