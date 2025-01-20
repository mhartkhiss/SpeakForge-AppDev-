package com.example.appdev.models;

public class TranslationHistory {
    private String originalText;
    private String translatedText;
    private String targetLanguage;
    private long timestamp;

    public TranslationHistory() {
        // Required empty constructor for Firebase
    }

    public TranslationHistory(String originalText, String translatedText, String targetLanguage) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.targetLanguage = targetLanguage;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }
    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 