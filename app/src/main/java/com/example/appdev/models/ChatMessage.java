package com.example.appdev.models;

public class ChatMessage {
    private String messageId;
    private String message;
    private String messageOG;
    private long timestamp;
    private String senderId; // Add sender ID field
    private String messageVar1;
    private String messageVar2;
    private String messageVar3;
    private String messageVar4;
    private String messageVar5;


    public ChatMessage() {
        // Default constructor required for Firebase
    }

    public ChatMessage(String messageId, String message, String messageOG, long timestamp, String senderId) {
        this.messageId = messageId;
        this.message = message;
        this.messageOG = messageOG;
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

    public String getMessageOG() {
        return messageOG;
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

    // add getter and setter for messageVar1, messageVar2, messageVar3, messageVar4, and messageVar5

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public String getMessageVar1() {
        return messageVar1;
    }

    public void setMessageVar1(String messageVar1) {
        this.messageVar1 = messageVar1;
    }

    public String getMessageVar2() {
        return messageVar2;
    }

    public void setMessageVar2(String messageVar2) {
        this.messageVar2 = messageVar2;
    }

    public String getMessageVar3() {
        return messageVar3;
    }

    public void setMessageVar3(String messageVar3) {
        this.messageVar3 = messageVar3;
    }

    public String getMessageVar4() {
        return messageVar4;
    }

    public void setMessageVar4(String messageVar4) {
        this.messageVar4 = messageVar4;
    }

    public String getMessageVar5() {
        return messageVar5;
    }

    public void setMessageVar5(String messageVar5) {
        this.messageVar5 = messageVar5;
    }
}



