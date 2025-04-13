package com.example.appdev.models;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String messageId;
    private String message;
    private long timestamp;
    private String senderId;
    private String senderLanguage;
    private String translationMode;
    private Map<String, String> translations;

    public Message() {
        // Default constructor required for Firebase
        translations = new HashMap<>();
    }

    public Message(String messageId, String message, long timestamp, String senderId) {
        this.messageId = messageId;
        this.message = message;
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.translations = new HashMap<>();
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
    
    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations != null ? translations : new HashMap<>();
    }
    
    // Helper methods to get specific translations
    public String getTranslation1() {
        return translations != null ? translations.get("translation1") : null;
    }
    
    public String getTranslation2() {
        return translations != null ? translations.get("translation2") : null;
    }
    
    public String getTranslation3() {
        return translations != null ? translations.get("translation3") : null;
    }
    
    // Helper methods to set specific translations
    public void setTranslation1(String translation) {
        if (translations == null) {
            translations = new HashMap<>();
        }
        translations.put("translation1", translation);
    }
    
    public void setTranslation2(String translation) {
        if (translations == null) {
            translations = new HashMap<>();
        }
        translations.put("translation2", translation);
    }
    
    public void setTranslation3(String translation) {
        if (translations == null) {
            translations = new HashMap<>();
        }
        translations.put("translation3", translation);
    }
}



