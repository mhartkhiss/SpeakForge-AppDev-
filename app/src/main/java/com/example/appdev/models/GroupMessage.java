package com.example.appdev.models;

import java.util.HashMap;
import java.util.Map;

public class GroupMessage extends Message {
    private String senderName;
    private String senderLanguage;
    private String senderProfileUrl;
    private Map<String, String> translations;

    public GroupMessage() {
        // Default constructor required for Firebase
        super();
        translations = new HashMap<>();
    }

    public GroupMessage(String messageId, String message, String messageOG, long timestamp, String senderId,
                       String senderName, String senderLanguage, String senderProfileUrl) {
        super(messageId, message, messageOG, timestamp, senderId);
        this.senderName = senderName;
        this.senderLanguage = senderLanguage;
        this.senderProfileUrl = senderProfileUrl;
        this.translations = new HashMap<>();
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderLanguage() {
        return senderLanguage;
    }

    public void setSenderLanguage(String senderLanguage) {
        this.senderLanguage = senderLanguage;
    }

    public String getSenderProfileUrl() {
        return senderProfileUrl;
    }

    public void setSenderProfileUrl(String senderProfileUrl) {
        this.senderProfileUrl = senderProfileUrl;
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations != null ? translations : new HashMap<>();
    }
}
