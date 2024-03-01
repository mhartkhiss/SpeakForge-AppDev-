package com.example.appdev.models;

public class ChatMessage {
    private String messageId;
    private String message;
    private long timestamp;
    private String senderId; // Add sender ID field

    public ChatMessage() {
        // Default constructor required for Firebase
    }

    public ChatMessage(String messageId, String message, long timestamp, String senderId) {
        this.messageId = messageId;
        this.message = message;
        this.timestamp = timestamp;
        this.senderId = senderId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
}



