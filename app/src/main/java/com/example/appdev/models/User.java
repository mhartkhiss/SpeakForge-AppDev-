package com.example.appdev.models;

public class User {
    private String userId;
    private String username;
    private String email;
    private String profileImageUrl; // Add profileImageUrl field
    private String targetLanguage, sourceLanguage;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String userId, String username, String email, String profileImageUrl) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl; // Initialize profileImageUrl field
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }
}

