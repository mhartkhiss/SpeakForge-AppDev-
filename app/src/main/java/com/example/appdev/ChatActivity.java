package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapters.ChatAdapter;
import com.example.appdev.models.Message;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.os.AsyncTask;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText chatBox;
    private ImageButton buttonSend;
    private ChatAdapter chatAdapter;
    private DatabaseReference messagesRef;
    private String roomId, recipientLanguage;
    private static final int SPEECH_REQUEST_CODE = 1;
    private DatabaseReference contactSettingsRef;
    private boolean translateEnabled = false;
    private int previousMessageCount = 0;
    private String recipientTranslator = "google"; // default value
    private String recipientId;
    
    // Reply UI elements
    private LinearLayout replyContainer;
    private TextView replyToSenderName;
    private TextView replyToMessageText;
    private ImageButton buttonCancelReply;


    //Establish Connection
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Add this line to adjust resize mode
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        setContentView(R.layout.activity_chat);

        // Retrieve recipient information from intent extras
        String recipientName = getIntent().getStringExtra("username");
        recipientLanguage = getIntent().getStringExtra("recipientLanguage");
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");

        // Store recipientId as class field
        recipientId = getIntent().getStringExtra("userId");
        
        // Add listener for recipient's translator preference
        DatabaseReference recipientTranslatorRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(recipientId)
                .child("translator");
                
        recipientTranslatorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    recipientTranslator = dataSnapshot.getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                recipientTranslator = "google"; // fallback to default
            }
        });

        // Generate a unique room ID for the conversation using the sender and recipient IDs
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        roomId = generateRoomId(senderId, recipientId);
        Variables.roomId = roomId;

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("messages");

        // Initialize views
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
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

        // Initialize RecyclerView
        chatAdapter = new ChatAdapter(messagesRef, roomId, this);

        recyclerViewChat.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);


        // Set click listener for send button
        buttonSend.setOnClickListener(v -> sendMessage(chatBox.getText().toString().trim(), recipientLanguage));

        ImageButton buttonMic = findViewById(R.id.buttonMic);
        buttonMic.setOnClickListener(v -> startSpeechRecognition());

        chatBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This method is called to notify you that, within s, the count characters
                // beginning at start are about to be replaced by new text with length after.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This method is called to notify you that, within s, the count characters
                // beginning at start have just replaced old text that had length before.
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
                // This method is called to notify you that, somewhere within s, the text has
                // been changed.
            }
        });
        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(v -> finish());

        loadMessages();

        DatabaseReference recipientLanguageRef = FirebaseDatabase.getInstance().getReference("users").child(recipientId).child("language");
        recipientLanguageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recipientLanguage = dataSnapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        de.hdodenhof.circleimageview.CircleImageView imageViewUserPicture = findViewById(R.id.imageViewUserPicture);

        if (profileImageUrl != null && !profileImageUrl.equals("none")) {
            Glide.with(this)
                    .load(profileImageUrl) // Replace with the URL or URI of the user's image
                    .into(imageViewUserPicture);
        } else {
            imageViewUserPicture.setImageResource(R.drawable.default_userpic);
        }

        TextView textViewUsername = findViewById(R.id.textViewUsername);
        textViewUsername.setText(recipientName);

        // Set click listener for more options button
        ImageView buttonMore = findViewById(R.id.buttonMore);
        buttonMore.setOnClickListener(v -> {
            // Get recipient email from Firebase
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(recipientId);
            
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String recipientEmail = snapshot.child("email").getValue(String.class);
                    
                    // Launch ContactSettingsActivity with all user info
                    Intent intent = new Intent(ChatActivity.this, ContactSettingsActivity.class);
                    intent.putExtra("username", recipientName);
                    intent.putExtra("email", recipientEmail);
                    intent.putExtra("language", recipientLanguage);
                    intent.putExtra("profileImageUrl", profileImageUrl);
                    intent.putExtra("userId", recipientId);
                    startActivity(intent);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Only show notification if the user is still logged in
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        CustomNotification.showNotification(ChatActivity.this,
                            "Failed to load user information", false);
                    }
                }
            });
        });

        // Initialize contact settings reference - check recipient's settings for the current user
        contactSettingsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(recipientId)  // Changed from senderId to recipientId
                .child("contactsettings")
                .child(senderId)     // Changed from recipientId to senderId
                .child("translateMessages");

        // Listen for translation setting changes
        contactSettingsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    translateEnabled = dataSnapshot.getValue(Boolean.class);
                } else {
                    translateEnabled = false; // Default to false if setting doesn't exist
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                translateEnabled = false;
            }
        });
    }

    private String generateRoomId(String senderId, String recipientId) {
        // Sort sender and recipient IDs alphabetically to ensure consistency
        String[] ids = {senderId, recipientId};
        Arrays.sort(ids);

        // Concatenate sender and recipient IDs to create the room ID
        return ids[0] + "_" + ids[1];
    }

    public void sendMessage(String message, String targetLanguage) {
        if (message.trim().isEmpty()) {
            return;
        }

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String messageId = messagesRef.child(roomId).push().getKey();
        long timestamp = System.currentTimeMillis();

        // Save message data to Firebase
        if (messageId != null) {
            // Create a map for the initial message data without translations
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", messageId);
            messageData.put("message", message); // Original message text
            messageData.put("timestamp", timestamp);
            messageData.put("senderId", senderId);
            messageData.put("senderLanguage", Variables.userLanguage); // Store sender's language
            messageData.put("translationMode", Variables.isFormalTranslationMode ? "formal" : "casual"); // Store translation mode (formal/casual)
            messageData.put("translationState", "TRANSLATING"); // Set initial state to TRANSLATING
            
            // Add reply information if replying to a message
            Message replyingToMessage = chatAdapter.getReplyingToMessage();
            if (replyingToMessage != null) {
                messageData.put("replyToMessageId", replyingToMessage.getMessageId());
                messageData.put("replyToSenderId", replyingToMessage.getSenderId());
                messageData.put("replyToMessage", replyingToMessage.getMessage());
            }

            // Save the message to Firebase
            messagesRef.child(roomId).child(messageId).setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    // Message saved successfully, now translate it
                    translateMessage(targetLanguage, message, messageId);
                    chatBox.setText("");
                    
                    // Clear reply UI after sending
                    if (chatAdapter.getReplyingToMessage() != null) {
                        cancelReply();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatActivity", "Failed to save message: " + e.getMessage());
                });
        }
    }

    private void translateMessage(String targetLanguage, String messageTextOG, String messageId) {
        Variables.openAiPrompt = 1; // Use standard translation setting for initial messages

        // Prepare the request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("text", messageTextOG);
            requestBody.put("source_language", Variables.userLanguage);
            requestBody.put("target_language", targetLanguage);
            requestBody.put("mode", Variables.isFormalTranslationMode ? "formal" : "casual");
            requestBody.put("variants", "single"); // Add variants parameter explicitly
            requestBody.put("translator", recipientTranslator);
            requestBody.put("room_id", roomId);
            requestBody.put("message_id", messageId);
            requestBody.put("update_state", true); // Tell API to update translationState
            
            String apiUrl = Variables.API_TRANSLATE_DB_URL;
            
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... voids) {
                    try {
                        URL url = new URL(apiUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);

                        try (OutputStream os = conn.getOutputStream()) {
                            byte[] input = requestBody.toString().getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }

                        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            StringBuilder response = new StringBuilder();
                            try (BufferedReader br = new BufferedReader(
                                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response.append(responseLine.trim());
                                }
                                
                                // With API_TRANSLATE_DB_URL, Firebase is updated directly by the server
                                return true;
                            }
                        } else {
                            // If translation fails, set state back to null
                            DatabaseReference messageRef = messagesRef.child(roomId).child(messageId);
                            messageRef.child("translationState").setValue(null);
                            return false;
                        }
                    } catch (Exception e) {
                        Log.e("ChatActivity", "Error translating message: " + e.getMessage());
                        // If translation fails, set state back to null
                        DatabaseReference messageRef = messagesRef.child(roomId).child(messageId);
                        messageRef.child("translationState").setValue(null);
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    if (!success) {
                        Log.e("ChatActivity", "Failed to translate message");
                    }
                }
            }.execute();
        } catch (Exception e) {
            Log.e("ChatActivity", "Error creating JSON request: " + e.getMessage());
            // If JSON creation fails, set state back to null
            DatabaseReference messageRef = messagesRef.child(roomId).child(messageId);
            messageRef.child("translationState").setValue(null);
        }
    }

    private String removeQuotationMarks(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        if (text.startsWith("&quot;") && text.endsWith("&quot;")) {
            text = text.substring(6, text.length() - 6);
        }
        return text;
    }
    
    /**
     * Shows the reply UI when a user chooses to reply to a message
     * @param message The message being replied to
     */
    public void showReplyingToUI(Message message) {
        if (replyContainer == null) return;
        
        // Show the reply container
        replyContainer.setVisibility(View.VISIBLE);
        
        // Set the sender name
        String senderId = message.getSenderId();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        if (senderId.equals(currentUserId)) {
            replyToSenderName.setText("You");
        } else {
            // Look up the username from Firebase
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
            userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String username = snapshot.getValue(String.class);
                    if (username != null && !username.isEmpty()) {
                        replyToSenderName.setText(username);
                    } else {
                        replyToSenderName.setText("User");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    replyToSenderName.setText("User");
                }
            });
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
        if (chatAdapter != null) {
            chatAdapter.clearReplyingToMessage();
        }
    }



    private void loadMessages() {
        String roomId = this.roomId;
        if (roomId != null) {
            messagesRef.child(roomId).orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Message> messages = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            messages.add(message);
                        }

                    }
                    chatAdapter.setMessages(messages);
                    
                    // Only scroll if new messages are added
                    int newSize = messages.size();
                    if (newSize > previousMessageCount) {
                        recyclerViewChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                    previousMessageCount = newSize;
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("ConversationModeActivity", "Error loading messages: " + databaseError.getMessage());
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

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == this.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                chatBox.setText(spokenText);

            }
        }
    }
    
    
    
    /**
     * Gets the RecyclerView for scrolling to messages
     * @return The RecyclerView instance
     */
    public RecyclerView getRecyclerView() {
        return recyclerViewChat;
    }
}
