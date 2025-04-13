package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapters.GroupChatAdapter;
import com.example.appdev.models.Group;
import com.example.appdev.models.GroupMessage;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GroupChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewGroupChat;
    private EditText chatBox;
    private ImageButton buttonSend;
    private GroupChatAdapter groupChatAdapter;
    private DatabaseReference groupMessagesRef;
    private DatabaseReference groupRef;
    private DatabaseReference userMemberRef;
    
    private ValueEventListener groupDetailsListener;
    private ValueEventListener messagesListener;
    private ValueEventListener membershipListener;
    
    private Group currentGroup;
    private String groupId;
    private String currentUserName;
    private String currentUserProfileUrl;
    private boolean isAdmin = false;
    private int previousMessageCount = 0;
    private boolean translateEnabled = true;
    
    private static final int SPEECH_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Adjust resize mode
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        setContentView(R.layout.activity_group_chat);

        // Retrieve group information from intent extras
        groupId = getIntent().getStringExtra("groupId");
        String groupName = getIntent().getStringExtra("groupName");
        String groupImageUrl = getIntent().getStringExtra("groupImageUrl");
        
        if (groupId == null) {
            CustomNotification.showNotification(this, "Error loading group chat", false);
            finish();
            return;
        }

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        groupMessagesRef = database.getReference("group_messages");
        groupRef = database.getReference("groups").child(groupId);
        
        // Get current user's profile information
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usersRef = database.getReference("users").child(currentUserId);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUserName = snapshot.child("username").getValue(String.class);
                currentUserProfileUrl = snapshot.child("profilePictureUrl").getValue(String.class);
                
                if (currentUserName == null) {
                    currentUserName = "Unknown User";
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                currentUserName = "Unknown User";
            }
        });
        
        // Load group details
        groupDetailsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if activity is still active
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                
                currentGroup = snapshot.getValue(Group.class);
                if (currentGroup == null) {
                    CustomNotification.showNotification(GroupChatActivity.this, 
                        "Group not found", false);
                    finish();
                    return;
                }
                
                // Check if current user is still a member of the group
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (currentGroup.getMembers() == null || !currentGroup.getMembers().containsKey(currentUserId)) {
                    CustomNotification.showNotification(GroupChatActivity.this, 
                        "You are no longer a member of this group", false);
                    finish();
                    return;
                }
                
                // Update UI with group details
                // Find views inside the included layout
                View headerView = findViewById(R.id.includeGroupHeader);
                TextView textViewGroupName = headerView.findViewById(R.id.textViewGroupName);
                textViewGroupName.setText(currentGroup.getName());
                
                de.hdodenhof.circleimageview.CircleImageView imageViewGroupPicture = 
                    headerView.findViewById(R.id.imageViewGroupPicture);
                
                if (currentGroup.getGroupImageUrl() != null && !currentGroup.getGroupImageUrl().isEmpty()) {
                    // Check again if activity is still active before loading image
                    if (!isFinishing() && !isDestroyed()) {
                        Glide.with(GroupChatActivity.this)
                            .load(currentGroup.getGroupImageUrl())
                            .placeholder(R.drawable.group_default_icon)
                            .into(imageViewGroupPicture);
                    }
                } else {
                    imageViewGroupPicture.setImageResource(R.drawable.group_default_icon);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                CustomNotification.showNotification(GroupChatActivity.this, 
                    "Failed to load group details", false);
            }
        };
        groupRef.addValueEventListener(groupDetailsListener);
        
        // Initialize views
        recyclerViewGroupChat = findViewById(R.id.recyclerViewGroupChat);
        chatBox = findViewById(R.id.chatBox);
        buttonSend = findViewById(R.id.buttonSend);
        
        // Initialize RecyclerView with adapter
        groupChatAdapter = new GroupChatAdapter(groupMessagesRef, groupId, this);
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewGroupChat.setLayoutManager(layoutManager);
        recyclerViewGroupChat.setAdapter(groupChatAdapter);
        
        // Set click listener for send button
        buttonSend.setOnClickListener(v -> sendGroupMessage(chatBox.getText().toString().trim()));
        
        // Set up microphone button
        ImageButton buttonMic = findViewById(R.id.buttonMic);
        buttonMic.setOnClickListener(v -> startSpeechRecognition());
        
        // Text watcher for chat box to toggle send/mic buttons
        chatBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() > 0) {
                    buttonMic.setVisibility(View.GONE);
                    buttonSend.setVisibility(View.VISIBLE);
                } else {
                    buttonMic.setVisibility(View.VISIBLE);
                    buttonSend.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });
        
        // Set up back button and info button in the included header
        View headerView = findViewById(R.id.includeGroupHeader);
        ImageView imageViewBack = headerView.findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(v -> finish());
        
        // Set up group info button
        ImageView buttonInfo = headerView.findViewById(R.id.buttonInfo);
        buttonInfo.setOnClickListener(v -> {
            Intent intent = new Intent(GroupChatActivity.this, GroupInfoActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
        
        // Load messages
        loadGroupMessages();
        
        // Initialize membership listener
        userMemberRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId).child("members").child(currentUserId);
        membershipListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    CustomNotification.showNotification(GroupChatActivity.this, "You are no longer a member of this group", false);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GroupChatActivity", "Failed to check membership: " + error.getMessage());
            }
        };
        userMemberRef.addValueEventListener(membershipListener);
    }
    
    private void sendGroupMessage(String messageText) {
        if (messageText.isEmpty() || groupId == null || currentUserName == null) {
            return;
        }
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Create a unique key for the message
        String messageId = groupMessagesRef.child(groupId).push().getKey();
        
        // Get current timestamp
        long timestamp = System.currentTimeMillis();
        
        // Create a HashMap to represent the message data
        HashMap<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        
        // Always show loading indicator while translating
        messageData.put("message", "......");
        
        messageData.put("messageOG", messageText);
        messageData.put("timestamp", timestamp);
        messageData.put("senderId", currentUserId);
        messageData.put("senderName", currentUserName);
        messageData.put("senderLanguage", Variables.userLanguage);
        messageData.put("senderProfileUrl", currentUserProfileUrl);
        messageData.put("sourceLanguage", Variables.userLanguage);
        messageData.put("translationMode", Variables.isFormalTranslationMode ? "formal" : "casual");
        
        // Save message to Firebase Database
        groupMessagesRef.child(groupId).child(messageId).setValue(messageData)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Always translate group messages
                    translateGroupMessage(messageText, messageId);
                } else {
                    Log.e("GroupChatActivity", "Failed to send message: " + task.getException());
                    CustomNotification.showNotification(this, "Failed to send message", false);
                }
            });
        
        // Clear the input field
        chatBox.setText("");
    }
    
    private void translateGroupMessage(String messageText, String messageId) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    // Prepare the request body
                    JSONObject requestBody = new JSONObject();
                    String messageTextQuoted = "\"" + messageText + "\"";
                    requestBody.put("text", messageTextQuoted);
                    requestBody.put("source_language", Variables.userLanguage);
                    requestBody.put("model", Variables.userTranslator.toLowerCase());
                    requestBody.put("group_id", groupId);
                    requestBody.put("message_id", messageId);
                    requestBody.put("translation_mode", Variables.isFormalTranslationMode ? "formal" : "casual");

                    // Make the API request
                    URL url = new URL(Variables.API_TRANSLATE_GROUP_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // Send request body
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    
                    return conn.getResponseCode() == HttpURLConnection.HTTP_OK;
                    
                } catch (Exception e) {
                    Log.e("GroupChatActivity", "Group translation error: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (!success) {
                    // If translation failed, update the message to use original text
                    groupMessagesRef.child(groupId).child(messageId)
                        .child("message").setValue(messageText);
                }
            }
        }.execute();
    }
    
    // Keep this method for individual language translation (used for regeneration)
    private void translateMessageToLanguage(String messageText, String messageId, String targetLanguage) {
        // Skip translation if target language is the same as source language
        if (targetLanguage.equals(Variables.userLanguage)) {
            // If language is the same, just use the original message without translation
            groupMessagesRef.child(groupId).child(messageId).child("message").setValue(messageText);
            
            // Also add it to the translations map for consistency
            groupMessagesRef.child(groupId).child(messageId)
                .child("translations")
                .child(targetLanguage)
                .setValue(messageText);
                
            return;
        }
        
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    // Prepare the request body
                    JSONObject requestBody = new JSONObject();
                    String messageTextQuoted = "\"" + messageText + "\"";
                    requestBody.put("text", messageTextQuoted);
                    requestBody.put("source_language", Variables.userLanguage);
                    requestBody.put("target_language", targetLanguage);
                    requestBody.put("mode", "single");
                    requestBody.put("model", Variables.userTranslator.toLowerCase());
                    requestBody.put("group_id", groupId);
                    requestBody.put("message_id", messageId);
                    requestBody.put("is_group", true);
                    requestBody.put("translation_mode", Variables.isFormalTranslationMode ? "formal" : "casual");

                    // Make the API request
                    URL url = new URL(Variables.API_TRANSLATE_DB_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // Send request body
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    return conn.getResponseCode() == HttpURLConnection.HTTP_OK;

                } catch (Exception e) {
                    Log.e("GroupChatActivity", "Translation error: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (!success) {
                    // If translation failed, update the message to use original text
                    groupMessagesRef.child(groupId).child(messageId)
                        .child("message").setValue(messageText);
                }
            }
        }.execute();
    }
    
    private void loadGroupMessages() {
        if (groupId != null) {
            messagesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Check if activity is still active
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    List<GroupMessage> messages = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        GroupMessage message = snapshot.getValue(GroupMessage.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    groupChatAdapter.setMessages(messages);
                    
                    // Only scroll if new messages are added
                    int newSize = messages.size();
                    if (newSize > previousMessageCount) {
                        recyclerViewGroupChat.scrollToPosition(groupChatAdapter.getItemCount() - 1);
                    }
                    previousMessageCount = newSize;
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("GroupChatActivity", "Error loading messages: " + databaseError.getMessage());
                }
            };
            groupMessagesRef.child(groupId).orderByChild("timestamp").addValueEventListener(messagesListener);
        }
    }
    
    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                chatBox.setText(spokenText);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up all listeners to prevent memory leaks and crashes
        if (membershipListener != null && userMemberRef != null) {
            userMemberRef.removeEventListener(membershipListener);
        }
        
        if (groupDetailsListener != null && groupRef != null) {
            groupRef.removeEventListener(groupDetailsListener);
        }
        
        if (messagesListener != null && groupMessagesRef != null && groupId != null) {
            groupMessagesRef.child(groupId).removeEventListener(messagesListener);
        }
    }
}
