package com.example.appdev.models;

public class ConnectionRequest {
    private String requestId;
    private String fromUserId;
    private String toUserId;
    private String sessionId;
    private String status; // PENDING, ACCEPTED, REJECTED, TIMEOUT, EXPIRED, CANCELLED
    private long timestamp;
    private String fromUserName;
    private String fromUserLanguage;
    private String fromUserProfileImageUrl;
    private long expiresAt;

    public ConnectionRequest() {
        // Default constructor required for Firebase
    }

    public ConnectionRequest(String requestId, String fromUserId, String toUserId,
                           String sessionId, String fromUserName, String fromUserLanguage,
                           String fromUserProfileImageUrl) {
        this.requestId = requestId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.sessionId = sessionId;
        this.status = "PENDING";
        this.timestamp = System.currentTimeMillis();
        this.fromUserName = fromUserName;
        this.fromUserLanguage = fromUserLanguage;
        this.fromUserProfileImageUrl = fromUserProfileImageUrl;
        // Request expires in 5 minutes
        this.expiresAt = this.timestamp + (5 * 60 * 1000);
    }

    // Getters and setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getFromUserLanguage() { return fromUserLanguage; }
    public void setFromUserLanguage(String fromUserLanguage) { this.fromUserLanguage = fromUserLanguage; }

    public String getFromUserProfileImageUrl() { return fromUserProfileImageUrl; }
    public void setFromUserProfileImageUrl(String fromUserProfileImageUrl) { this.fromUserProfileImageUrl = fromUserProfileImageUrl; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    // Helper methods
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isPending() {
        return "PENDING".equals(status) && !isExpired();
    }

    public boolean isAccepted() {
        return "ACCEPTED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }
}
