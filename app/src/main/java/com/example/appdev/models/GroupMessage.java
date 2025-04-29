package com.example.appdev.models;

import java.util.HashMap;
import java.util.Map;

public class GroupMessage extends Message {
    private String senderLanguage;
    private String senderProfileUrl;
    private Map<String, String> translations;
    private String replyToMessageId;
    private String replyToSenderId;
    private String replyToMessage;

    public GroupMessage() {
        // Default constructor required for Firebase
        super();
        translations = new HashMap<>();
        replyToMessageId = null;
        replyToSenderId = null;
        replyToMessage = null;
    }

    public GroupMessage(String messageId, String message, long timestamp, String senderId,
                       String senderLanguage, String senderProfileUrl) {
        super(messageId, message, timestamp, senderId);
        this.senderLanguage = senderLanguage;
        this.senderProfileUrl = senderProfileUrl;
        this.translations = new HashMap<>();
        this.replyToMessageId = null;
        this.replyToSenderId = null;
        this.replyToMessage = null;
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
    
    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getReplyToSenderId() {
        return replyToSenderId;
    }

    public void setReplyToSenderId(String replyToSenderId) {
        this.replyToSenderId = replyToSenderId;
    }

    public String getReplyToMessage() {
        return replyToMessage;
    }

    public void setReplyToMessage(String replyToMessage) {
        this.replyToMessage = replyToMessage;
    }
    
    /**
     * Checks if this message is a reply to another message
     * @return true if this message is a reply, false otherwise
     */
    public boolean isReply() {
        return replyToMessageId != null && !replyToMessageId.isEmpty();
    }
}
