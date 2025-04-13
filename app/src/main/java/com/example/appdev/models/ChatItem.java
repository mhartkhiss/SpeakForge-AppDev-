package com.example.appdev.models;

/**
 * Model class representing a chat item in the chat list.
 * Can be either a user chat or a group chat.
 */
public class ChatItem {
    private String id;
    private String name;
    private String imageUrl;
    private String lastMessage;
    private String lastMessageOG;
    private String lastMessageSenderId;
    private long lastMessageTime;
    private boolean isGroup;

    public ChatItem() {
        // Default constructor for Firebase
    }

    // Constructor for user chat
    public ChatItem(User user) {
        this.id = user.getUserId();
        this.name = user.getUsername();
        this.imageUrl = user.getProfileImageUrl();
        
        // Initialize with empty strings to avoid null values
        this.lastMessageSenderId = "";
        this.lastMessage = "";
        this.lastMessageOG = "";
        
        if (user.getLastMessage() != null && !user.getLastMessage().isEmpty()) {
            String[] messageParts = user.getLastMessage().split("\\|", 3);
            if (messageParts.length == 3) {
                this.lastMessageSenderId = messageParts[0];
                this.lastMessage = messageParts[1];
                this.lastMessageOG = messageParts[2];
            } else if (messageParts.length == 2) {
                this.lastMessageSenderId = messageParts[0];
                this.lastMessage = messageParts[1];
                this.lastMessageOG = messageParts[1]; // Use the message as original if no OG available
            } else if (messageParts.length == 1) {
                this.lastMessage = messageParts[0];
                this.lastMessageOG = messageParts[0]; // Use the message as original
            }
        }
        this.lastMessageTime = user.getLastMessageTime();
        this.isGroup = false;
    }

    // Constructor for group chat
    public ChatItem(Group group) {
        this.id = group.getGroupId();
        this.name = group.getName();
        this.imageUrl = group.getGroupImageUrl();
        this.lastMessage = group.getLastMessage() != null ? group.getLastMessage() : "";
        this.lastMessageTime = group.getLastMessageTime();
        this.lastMessageSenderId = group.getLastMessageSenderId() != null ? group.getLastMessageSenderId() : "";
        this.lastMessageOG = group.getLastMessageOG() != null ? group.getLastMessageOG() : "";
        this.isGroup = true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageOG() {
        return lastMessageOG;
    }

    public void setLastMessageOG(String lastMessageOG) {
        this.lastMessageOG = lastMessageOG;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }
}
