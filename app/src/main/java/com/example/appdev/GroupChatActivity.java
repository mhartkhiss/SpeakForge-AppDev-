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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

public class GroupChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewGroupChat;
    private EditText chatBox;
    private ImageButton buttonSend;
    private GroupChatAdapter groupChatAdapter;
    private DatabaseReference groupMessagesRef;
    private String groupId;
    private Group currentGroup;
    private static final int SPEECH_REQUEST_CODE = 1;
    private int previousMessageCount = 0;
    private String currentUserName;
    private String currentUserProfileUrl;
    private ValueEventListener membershipListener;
    private DatabaseReference userMemberRef;
    
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
        DatabaseReference groupRef = database.getReference("groups").child(groupId);
        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                TextView textViewGroupName = findViewById(R.id.textViewGroupName);
                textViewGroupName.setText(currentGroup.getName());
                
                de.hdodenhof.circleimageview.CircleImageView imageViewGroupPicture = 
                    findViewById(R.id.imageViewGroupPicture);
                
                if (currentGroup.getGroupImageUrl() != null && !currentGroup.getGroupImageUrl().isEmpty()) {
                    Glide.with(GroupChatActivity.this)
                        .load(currentGroup.getGroupImageUrl())
                        .placeholder(R.drawable.group_default_icon)
                        .into(imageViewGroupPicture);
                } else {
                    imageViewGroupPicture.setImageResource(R.drawable.group_default_icon);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                CustomNotification.showNotification(GroupChatActivity.this, 
                    "Failed to load group details", false);
            }
        });

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
        
        // Set up back button
        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(v -> finish());
        
        // Set up group info button
        ImageView buttonInfo = findViewById(R.id.buttonInfo);
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
        messageData.put("message", messageText);
        messageData.put("messageOG", messageText);
        messageData.put("timestamp", timestamp);
        messageData.put("senderId", currentUserId);
        messageData.put("senderName", currentUserName);
        messageData.put("senderLanguage", Variables.userLanguage);
        messageData.put("senderProfileUrl", currentUserProfileUrl);
        messageData.put("sourceLanguage", Variables.userLanguage);
        
        // Save message to Firebase Database
        groupMessagesRef.child(groupId).child(messageId).setValue(messageData)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // For each group member, translate the message to their language if needed
                    if (currentGroup != null && currentGroup.getMembers() != null) {
                        for (Map.Entry<String, Boolean> member : currentGroup.getMembers().entrySet()) {
                            String memberId = member.getKey();
                            
                            // Skip translation for the sender
                            if (memberId.equals(currentUserId)) {
                                continue;
                            }
                            
                            // Get member's language preference
                            DatabaseReference memberRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(memberId);
                                
                            memberRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String memberLanguage = snapshot.child("language").getValue(String.class);
                                    String memberTranslator = snapshot.child("translator").getValue(String.class);
                                    
                                    // Check if translation is needed
                                    if (memberLanguage != null && 
                                        !memberLanguage.equals(Variables.userLanguage)) {
                                        
                                        // Handle translator preference, default to "google"
                                        if (memberTranslator == null) {
                                            memberTranslator = "google";
                                        }
                                        
                                        // Store the original message text with quotes for API
                                        String messageTextQuoted = "\"" + messageText + "\"";
                                        
                                        // Translate the message for this member
                                        translateGroupMessage(
                                            memberLanguage, 
                                            messageTextQuoted, 
                                            messageId, 
                                            memberId,
                                            memberTranslator
                                        );
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("GroupChatActivity", "Failed to get member language: " + error.getMessage());
                                }
                            });
                        }
                    }
                } else {
                    Log.e("GroupChatActivity", "Failed to send message: " + task.getException());
                    CustomNotification.showNotification(this, "Failed to send message", false);
                }
            });
        
        // Clear the input field
        chatBox.setText("");
    }
    
    private void translateGroupMessage(String targetLanguage, String messageTextOG, 
                                     String messageId, String memberId, String translatorModel) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    // Prepare the request body
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("text", messageTextOG);
                    requestBody.put("source_language", Variables.userLanguage);
                    requestBody.put("target_language", targetLanguage);
                    requestBody.put("mode", "group");
                    requestBody.put("model", translatorModel.toLowerCase());
                    requestBody.put("group_id", groupId);
                    requestBody.put("message_id", messageId);
                    requestBody.put("member_id", memberId);

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
                    // If translation fails, log the error
                    Log.e("GroupChatActivity", "Failed to translate message for member: " + memberId);
                }
            }
        }.execute();
    }
    
    private void loadGroupMessages() {
        if (groupId != null) {
            groupMessagesRef.child(groupId).orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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
                });
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
        if (membershipListener != null && userMemberRef != null) {
            userMemberRef.removeEventListener(membershipListener);
        }
    }
}
