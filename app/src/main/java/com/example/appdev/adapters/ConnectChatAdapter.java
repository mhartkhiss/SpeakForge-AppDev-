package com.example.appdev.adapters;

import android.content.Context;
import android.util.Log;

import com.example.appdev.models.Message;
import com.google.firebase.database.DatabaseReference;

public class ConnectChatAdapter extends ChatAdapter {

    private static final String TAG = "ConnectChatAdapter";

    public ConnectChatAdapter(DatabaseReference messagesRef, String sessionId, Context context) {
        super(messagesRef, sessionId, context);
        Log.d(TAG, "ConnectChatAdapter created for session: " + sessionId);
    }

    @Override
    public void setMessages(java.util.List<Message> messages) {
        // Log voice message information for debugging
        if (messages != null) {
            for (Message message : messages) {
                if (message != null && message.isVoiceMessage()) {
                    Log.d(TAG, "Voice message found: " + message.getMessageId() +
                          " from sender: " + message.getSenderId());
                }
            }
        }
        super.setMessages(messages);
    }
}
