package com.example.appdev.models;

public class User {
    private String userId;
    private String username;
    private String email;
    private String profileImageUrl;
    private String language;
    private String accountType;
    private String createdAt;
    private String lastLoginDate;
    private String translator;
    private String apiKey;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String userId, String username, String email, String profileImageUrl, String accountType,
                String language, String createdAt, String lastLoginDate, String translator, String apiKey) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.accountType = accountType;
        this.language = language;
        this.createdAt = createdAt;
        this.lastLoginDate = lastLoginDate;
        this.translator = translator;
        this.apiKey = apiKey;
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

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setLastLoginDate(String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setTranslator(String translator) {
        this.translator = translator;
    }

    public String getTranslator() {
        return translator;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

}

