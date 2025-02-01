package com.example.appdev.models;

public class TranslationHistory {
    private String originalText;
    private String translatedText;
    private String sourceLanguage;
    private String targetLanguage;
    private String translator;
    private long timestamp;

    public TranslationHistory() {
        // Required empty constructor for Firebase
    }

    public TranslationHistory(String originalText, String translatedText, 
                            String sourceLanguage, String targetLanguage, String translator) {
        this.originalText = originalText;
        this.translatedText = translatedText;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.translator = translator;
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
    public String getSourceLanguage() { 
        return sourceLanguage != null ? sourceLanguage : "English"; 
    }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }
    public String getTranslator() { 
        return translator != null ? translator : "google"; 
    }
    public void setTranslator(String translator) { this.translator = translator; }
} 