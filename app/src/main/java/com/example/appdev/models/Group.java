package com.example.appdev.models;

import java.util.HashMap;
import java.util.Map;

public class Group {
    private String groupId;
    private String name;
    private String description;
    private long createdAt;
    private String createdBy;
    private Map<String, Boolean> members;  // Map of userId to boolean (true means admin)
    private String groupImageUrl;
    private String defaultLanguage;
    private String lastMessage;
    private long lastMessageTime;
    private String lastMessageSenderId;
    private String lastMessageOG;

    public Group() {
        // Default constructor required for Firebase
        this.members = new HashMap<>();
    }

    public Group(String groupId, String name, String description, long createdAt, String createdBy) {
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.members = new HashMap<>();
        this.members.put(createdBy, true); // Creator is admin by default
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    public void addMember(String userId) {
        if (members == null) {
            members = new HashMap<>();
        }
        members.put(userId, false);
    }

    public void addAdmin(String userId) {
        if (members == null) {
            members = new HashMap<>();
        }
        members.put(userId, true);
    }

    public void removeMember(String userId) {
        if (members != null) {
            members.remove(userId);
        }
    }

    public boolean isAdmin(String userId) {
        if (members != null && members.containsKey(userId)) {
            return Boolean.TRUE.equals(members.get(userId));
        }
        return false;
    }

    public boolean isMember(String userId) {
        return members != null && members.containsKey(userId);
    }

    public String getGroupImageUrl() {
        return groupImageUrl;
    }

    public void setGroupImageUrl(String groupImageUrl) {
        this.groupImageUrl = groupImageUrl;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getLastMessageOG() {
        return lastMessageOG;
    }

    public void setLastMessageOG(String lastMessageOG) {
        this.lastMessageOG = lastMessageOG;
    }
}
