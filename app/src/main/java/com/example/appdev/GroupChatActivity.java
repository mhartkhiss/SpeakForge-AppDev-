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
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapters.GroupChatAdapter;
import com.example.appdev.models.Group;
import com.example.appdev.models.GroupMessage;
import com.example.appdev.utils.CustomNotification;
import com.example.appdev.utils.TranslationContextManager;
import com.example.appdev.utils.TranslationModeManager;
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
    
    // Reply UI elements
    private LinearLayout replyContainer;
    private TextView replyToSenderName;
    private TextView replyToMessageText;
    private ImageButton buttonCancelReply;
    
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
        
        // Initialize reply UI elements
        replyContainer = findViewById(R.id.replyContainer);
        replyToSenderName = findViewById(R.id.replyToSenderName);
        replyToMessageText = findViewById(R.id.replyToMessageText);
        buttonCancelReply = findViewById(R.id.buttonCancelReply);
        
        // Set up cancel reply button
        if (buttonCancelReply != null) {
            buttonCancelReply.setOnClickListener(v -> cancelReply());
        }
        
        // Set chatbox hint with user's language
        chatBox.setHint("Type a message in " + Variables.userLanguage + "...");
        
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
        
        // Set up the menu button
        ImageView buttonMenu = headerView.findViewById(R.id.buttonMenu);
        buttonMenu.setOnClickListener(v -> {
            showPopupMenu(buttonMenu);
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

        // Initialize translation mode and context settings
        TranslationModeManager.initializeFromPreferences(this);
        TranslationContextManager.initializeFromPreferences(this);
    }
    
    private void sendGroupMessage(String messageText) {
        if (TextUtils.isEmpty(messageText)) {
            return;
        }
        
        // Generate a unique ID for the message
        String messageId = groupMessagesRef.child(groupId).push().getKey();
        
        // Create a timestamp for the message
        long timestamp = System.currentTimeMillis();
        
        // Get current user ID
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Create message data with loading placeholder
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        messageData.put("message", messageText);
        messageData.put("timestamp", timestamp);
        messageData.put("senderId", senderId);
        messageData.put("senderLanguage", Variables.userLanguage);
        
        // Initialize empty translations map to show loading state
        messageData.put("translations", new HashMap<>());
        
        // Add profile image URL if available
        if (currentUserProfileUrl != null) {
            messageData.put("senderProfileUrl", currentUserProfileUrl);
        }
        
        // Add reply information if replying to a message
        GroupMessage replyingToMessage = groupChatAdapter.getReplyingToMessage();
        if (replyingToMessage != null) {
            messageData.put("replyToMessageId", replyingToMessage.getMessageId());
            messageData.put("replyToSenderId", replyingToMessage.getSenderId());
            messageData.put("replyToMessage", replyingToMessage.getMessage());
        }
        
        Log.d("GroupChatActivity", "Sending message: " + messageId);
        
        // Save message to Firebase with empty translations
        groupMessagesRef.child(groupId).child(messageId).setValue(messageData)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Always translate group messages
                    Log.d("GroupChatActivity", "Message saved, now translating: " + messageId);
                    translateGroupMessage(messageText, messageId);
                    
                    // Clear reply UI after sending
                    if (groupChatAdapter.getReplyingToMessage() != null) {
                        cancelReply();
                    }
                } else {
                    Log.e("GroupChatActivity", "Failed to send message: " + task.getException());
                    CustomNotification.showNotification(this, "Failed to send message", false);
                }
            });
        
        // Clear the input field
        chatBox.setText("");
    }
    
    private void translateGroupMessage(String messageText, String messageId) {
        Log.d("GroupChatActivity", "Starting translation for message: " + messageId);
        
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
                    
                    // Add context-aware translation parameters
                    requestBody.put("use_context", Variables.isContextAwareTranslation);
                    requestBody.put("context_depth", Variables.contextDepth);

                    // Make the API request to the appropriate endpoint
                    String apiUrl = Variables.isContextAwareTranslation ? 
                            Variables.API_TRANSLATE_GROUP_CONTEXT_URL : 
                            Variables.API_TRANSLATE_GROUP_URL;
                    
                    Log.d("GroupChatActivity", "Using API URL: " + apiUrl);
                    Log.d("GroupChatActivity", "Request body: " + requestBody.toString());
                    
                    URL url = new URL(apiUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(30000); // 30 seconds
                    conn.setReadTimeout(30000); // 30 seconds

                    // Send request body
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    
                    int responseCode = conn.getResponseCode();
                    Log.d("GroupChatActivity", "Response code: " + responseCode);
                    
                    // Read response for debugging
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        StringBuilder response = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                            String responseLine;
                            while ((responseLine = br.readLine()) != null) {
                                response.append(responseLine.trim());
                            }
                        }
                        Log.e("GroupChatActivity", "Error response: " + response.toString());
                        return false;
                    }
                    
                    return true;
                    
                } catch (Exception e) {
                    Log.e("GroupChatActivity", "Group translation error: " + e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (!success) {
                    // If translation failed, update the message to use original text
                    Log.e("GroupChatActivity", "Translation failed, setting original text");
                    
                    // Instead of just setting the message field, make sure translations map has the original language
                    // so the original message in sender's language is shown correctly
                    DatabaseReference messageRef = groupMessagesRef.child(groupId).child(messageId);
                    
                    // Update message field to original text
                    messageRef.child("message").setValue(messageText);
                    
                    // Make sure original text is in translations map
                    messageRef.child("translations").child(Variables.userLanguage).setValue(messageText);
                } else {
                    Log.d("GroupChatActivity", "Translation completed successfully");
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
                    DatabaseReference messageRef = groupMessagesRef.child(groupId).child(messageId);
                    messageRef.child("message").setValue(messageText);
                    
                    // Make sure original text is in translations map
                    messageRef.child("translations").child(Variables.userLanguage).setValue(messageText);
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

    /**
     * Show popup menu with options for translation mode and context settings
     */
    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.menu_group_chat);
        
        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.action_group_info) {
                // Launch group info activity
                Intent intent = new Intent(this, GroupInfoActivity.class);
                intent.putExtra("GROUP_ID", groupId);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_translation_mode) {
                // Show translation mode dialog
                TranslationModeManager.showTranslationModeDialog(this, (isFormalMode) -> {
                    // Translation mode changed
                    TranslationModeManager.saveToPreferences(this, isFormalMode);
                });
                return true;
            } else if (id == R.id.action_context_settings) {
                // Show context settings dialog
                TranslationContextManager.showContextSettingsDialog(this, (isEnabled, contextDepth) -> {
                    // Context settings changed
                    TranslationContextManager.saveToPreferences(this, isEnabled, contextDepth);
                });
                return true;
            }
            
            return false;
        });
        
        popupMenu.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_group_info) {
            // Launch group info activity
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("GROUP_ID", groupId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_translation_mode) {
            // Show translation mode dialog
            TranslationModeManager.showTranslationModeDialog(this, (isFormalMode) -> {
                // Translation mode changed
                TranslationModeManager.saveToPreferences(this, isFormalMode);
            });
            return true;
        } else if (id == R.id.action_context_settings) {
            // Show context settings dialog
            TranslationContextManager.showContextSettingsDialog(this, (isEnabled, contextDepth) -> {
                // Context settings changed
                TranslationContextManager.saveToPreferences(this, isEnabled, contextDepth);
            });
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Shows the reply UI when a user chooses to reply to a message
     * @param message The message being replied to
     */
    public void showReplyingToUI(GroupMessage message) {
        if (replyContainer == null) return;
        
        // Show the reply container
        replyContainer.setVisibility(View.VISIBLE);
        
        // Set the sender name
        String senderId = message.getSenderId();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        if (senderId.equals(currentUserId)) {
            replyToSenderName.setText("You");
        } else {
            // Look up the username from the adapter's cache
            String username = groupChatAdapter.getUsernameFromCache(senderId);
            if (username != null && !username.isEmpty()) {
                replyToSenderName.setText(username);
            } else {
                replyToSenderName.setText("User");
            }
        }
        
        // Set the message text (truncate if too long)
        String messageText = message.getMessage();
        if (messageText != null) {
            if (messageText.length() > 50) {
                messageText = messageText.substring(0, 47) + "...";
            }
            replyToMessageText.setText(messageText);
        } else {
            replyToMessageText.setText("[Message unavailable]");
        }
        
        // Focus on the chat box
        chatBox.requestFocus();
    }
    
    /**
     * Cancels the current reply action
     */
    private void cancelReply() {
        if (replyContainer != null) {
            replyContainer.setVisibility(View.GONE);
        }
        
        // Clear the replying to message in the adapter
        if (groupChatAdapter != null) {
            groupChatAdapter.clearReplyingToMessage();
        }
    }
    
    /**
     * Gets the RecyclerView for scrolling to messages
     * @return The RecyclerView instance
     */
    public RecyclerView getRecyclerView() {
        return recyclerViewGroupChat;
    }
}
