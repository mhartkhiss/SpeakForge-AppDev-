package com.example.appdev.models;

public class VoiceMessage {
    private String messageId;
    private String voiceText;
    private String translatedText;
    private long timestamp;
    private String senderId;
    private String senderLanguage;
    private String translationMode;
    private String translationState;

    public VoiceMessage() {
        // Default constructor required for Firebase
    }

    public VoiceMessage(String messageId, String voiceText, String translatedText,
                       long timestamp, String senderId, String senderLanguage,
                       String translationMode, String translationState) {
        this.messageId = messageId;
        this.voiceText = voiceText;
        this.translatedText = translatedText;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.senderLanguage = senderLanguage;
        this.translationMode = translationMode;
        this.translationState = translationState;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getVoiceText() {
        return voiceText;
    }

    public void setVoiceText(String voiceText) {
        this.voiceText = voiceText;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
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

    public String getSenderLanguage() {
        return senderLanguage;
    }

    public void setSenderLanguage(String senderLanguage) {
        this.senderLanguage = senderLanguage;
    }

    public String getTranslationMode() {
        return translationMode;
    }

    public void setTranslationMode(String translationMode) {
        this.translationMode = translationMode;
    }

    public String getTranslationState() {
        return translationState;
    }

    public void setTranslationState(String translationState) {
        this.translationState = translationState;
    }
}
