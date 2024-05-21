package com.example.appdev.models;

public class Message {
    private String messageId;
    private String message;
    private String messageOG;
    private long timestamp;
    private String senderId;
    private String messageVar1;
    private String messageVar2;
    private String messageVar3;


    public Message() {
        // Default constructor required for Firebase
    }

    public Message(String messageId, String message, String messageOG, long timestamp, String senderId) {
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

}



