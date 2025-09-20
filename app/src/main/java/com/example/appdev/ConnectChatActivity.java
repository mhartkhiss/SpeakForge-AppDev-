package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapters.ChatAdapter;
import com.example.appdev.adapters.ConnectChatAdapter;
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
import java.util.Map;

import android.os.AsyncTask;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConnectChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private ImageButton buttonMic;
    private com.google.android.material.textfield.TextInputEditText textInputMessage;
    private ImageButton buttonSendText;
    private LinearLayout textInputContainer;
    private ChatAdapter chatAdapter;
    private DatabaseReference messagesRef;
    private String sessionId, recipientLanguage;
    private static final int SPEECH_REQUEST_CODE = 1;
    private DatabaseReference contactSettingsRef;
    private boolean translateEnabled = false;
    private int previousMessageCount = 0;
    private String recipientTranslator = "google"; // default value
    private String recipientId;
    private long sessionStartTime;
    private boolean sessionEnded = false;

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

        setContentView(R.layout.activity_connect_chat);

        // Retrieve recipient information from intent extras
        String recipientName = getIntent().getStringExtra("username");
        recipientLanguage = getIntent().getStringExtra("recipientLanguage");
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");

        // Store recipientId as class field
        recipientId = getIntent().getStringExtra("userId");

        // Set session start time
        sessionStartTime = System.currentTimeMillis();

        // Check if sessionId is provided (from connection request)
        String providedSessionId = getIntent().getStringExtra("sessionId");
        if (providedSessionId != null) {
            sessionId = providedSessionId;
            Variables.connectSessionId = sessionId;

            // Set this session as active to prevent duplicate openings
            com.example.appdev.utils.ConnectionRequestManager.getInstance().setActiveSessionId(sessionId);
        }

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

        // Generate a unique session ID for the conversation using the sender and recipient IDs
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        sessionId = generateSessionId(senderId, recipientId);
        Variables.connectSessionId = sessionId;

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("connect_chats");

        // Initialize views
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        buttonMic = findViewById(R.id.buttonMic);

        // Initialize text input elements (for testing)
        textInputMessage = findViewById(R.id.textInputMessage);
        buttonSendText = findViewById(R.id.buttonSendText);
        textInputContainer = findViewById(R.id.textInputContainer);

        // Initialize reply UI elements
        replyContainer = findViewById(R.id.replyContainer);
        replyToSenderName = findViewById(R.id.replyToSenderName);
        replyToMessageText = findViewById(R.id.replyToMessageText);
        buttonCancelReply = findViewById(R.id.buttonCancelReply);

        // Set up cancel reply button
        if (buttonCancelReply != null) {
            buttonCancelReply.setOnClickListener(v -> cancelReply());
        }

        // Initialize RecyclerView
        chatAdapter = new ConnectChatAdapter(messagesRef, sessionId, this);

        recyclerViewChat.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);

        // Set click listener for mic button
        buttonMic.setOnClickListener(v -> startSpeechRecognition());

        // Set click listener for text send button
        if (buttonSendText != null) {
            buttonSendText.setOnClickListener(v -> sendTextMessage());
        }

        // Set up text input enter key handling
        if (textInputMessage != null) {
            textInputMessage.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                    sendTextMessage();
                    return true;
                }
                return false;
            });
        }

        // Back button removed - no longer needed

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

        // Set click listener for end session button
        ImageView buttonEndSession = findViewById(R.id.buttonEndSession);
        buttonEndSession.setOnClickListener(v -> {
            endSession();
        });

        // Initialize contact settings reference - check recipient's settings for the current user
        contactSettingsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(recipientId);  // Changed from senderId to recipientId

        // Listen for session end messages
        listenForSessionEnd();

        contactSettingsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(recipientId)
                .child("contactsettings")
                .child(senderId)
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

    private String generateSessionId(String senderId, String recipientId) {
        // Sort sender and recipient IDs alphabetically to ensure consistency
        String[] ids = {senderId, recipientId};
        Arrays.sort(ids);

        // Concatenate sender and recipient IDs to create the session ID
        return ids[0] + "_" + ids[1];
    }

    public void sendMessage(String message, String targetLanguage) {
        if (message.trim().isEmpty()) {
            return;
        }

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String messageId = messagesRef.child(sessionId).push().getKey();
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
            messageData.put("isVoiceMessage", true); // Mark as voice message
            messageData.put("voiceText", message); // Store the transcribed voice text

            // Add reply information if replying to a message
            Message replyingToMessage = chatAdapter.getReplyingToMessage();
            if (replyingToMessage != null) {
                messageData.put("replyToMessageId", replyingToMessage.getMessageId());
                messageData.put("replyToSenderId", replyingToMessage.getSenderId());
                messageData.put("replyToMessage", replyingToMessage.getMessage());
            }

            // Save the message to Firebase
            messagesRef.child(sessionId).child(messageId).setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    // Message saved successfully, now translate it
                    translateMessage(targetLanguage, message, messageId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ConnectChatActivity", "Failed to save message: " + e.getMessage());
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
            requestBody.put("session_id", sessionId);
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
                            DatabaseReference messageRef = messagesRef.child(sessionId).child(messageId);
                            messageRef.child("translationState").setValue(null);
                            return false;
                        }
                    } catch (Exception e) {
                        Log.e("ConnectChatActivity", "Error translating message: " + e.getMessage());
                        // If translation fails, set state back to null
                        DatabaseReference messageRef = messagesRef.child(sessionId).child(messageId);
                        messageRef.child("translationState").setValue(null);
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    if (!success) {
                        Log.e("ConnectChatActivity", "Failed to translate message");
                    }
                }
            }.execute();
        } catch (Exception e) {
            Log.e("ConnectChatActivity", "Error creating JSON request: " + e.getMessage());
            // If JSON creation fails, set state back to null
            DatabaseReference messageRef = messagesRef.child(sessionId).child(messageId);
            messageRef.child("translationState").setValue(null);
        }
    }

    /**
     * Send a text message (for testing purposes when voice input is not available)
     */
    private void sendTextMessage() {
        if (textInputMessage == null) return;

        String messageText = textInputMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            return;
        }

        // Clear the input field
        textInputMessage.setText("");

        // Send the message using the same logic as voice messages
        sendMessage(messageText, recipientLanguage);

        // Hide keyboard
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
            getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
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

        // Focus on the mic button
        buttonMic.requestFocus();
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
        if (sessionId != null) {
            messagesRef.child(sessionId).orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Message> messages = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Message message = snapshot.getValue(Message.class);
                        if (message != null) {
                            // Mark this message as a voice message for the ConnectChatActivity
                            message.setIsVoiceMessage(true);
                            message.setVoiceText(message.getMessage());
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
                    Log.e("ConnectChatActivity", "Error loading messages: " + databaseError.getMessage());
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
                // Send the transcribed voice message
                sendMessage(spokenText, recipientLanguage);
            }
        }
    }



    /**
     * End the current session
     */
    private void endSession() {
        // Show confirmation dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("End Session");
        builder.setMessage("Are you sure you want to end this session? The other user will be notified.");
        builder.setPositiveButton("End Session", (dialog, which) -> {
            // Send session end message
            sendSessionEndMessage();

            // Clear active session
            if (sessionId != null) {
                com.example.appdev.utils.ConnectionRequestManager.getInstance().clearActiveSessionId();
            }

            // Finish activity
            finish();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Send a message indicating the session has ended
     */
    private void sendSessionEndMessage() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String messageText = "has left the session";

        // Create session end message
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messagesRef.push().getKey());
        messageData.put("senderId", currentUserId);
        messageData.put("messageText", messageText);
        messageData.put("timestamp", System.currentTimeMillis());
        messageData.put("isVoiceMessage", false);
        messageData.put("isSessionEnd", true); // Special flag for session end messages

        // Add to Firebase
        messagesRef.push().setValue(messageData);
    }

    /**
     * Listen for session end messages from the other user (only for this session)
     */
    private void listenForSessionEnd() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    String senderId = messageSnapshot.child("senderId").getValue(String.class);
                    Boolean isSessionEnd = messageSnapshot.child("isSessionEnd").getValue(Boolean.class);
                    Long messageTimestamp = messageSnapshot.child("timestamp").getValue(Long.class);

                    // Check if this is a session end message from the other user
                    if (senderId != null && !senderId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) &&
                        isSessionEnd != null && isSessionEnd) {

                        // Check if it's from the current session (newer than session start time)
                        // Add a 2-second buffer to account for timing differences
                        if (messageTimestamp != null && messageTimestamp > (sessionStartTime - 2000)) {
                            Log.d("ConnectChatActivity", "Found session end message from current session: sender=" + senderId +
                                  ", timestamp=" + messageTimestamp + ", sessionStart=" + sessionStartTime);

                            // Check if session has already ended to prevent duplicate notifications
                            if (!sessionEnded) {
                                sessionEnded = true;
                                Log.d("ConnectChatActivity", "Processing session end message from current session");

                                // The other user has ended the current session
                                runOnUiThread(() -> {
                                    // Show notification
                                    com.example.appdev.utils.CustomNotification.showNotification(
                                        ConnectChatActivity.this,
                                        "The other user has left the session", false);

                                    // Disable voice input
                                    if (buttonMic != null) {
                                        buttonMic.setEnabled(false);
                                        buttonMic.setAlpha(0.5f);
                                    }

                                    // Disable text input
                                    if (textInputMessage != null) {
                                        textInputMessage.setEnabled(false);
                                        textInputMessage.setHint("Session ended");
                                    }
                                    if (buttonSendText != null) {
                                        buttonSendText.setEnabled(false);
                                        buttonSendText.setAlpha(0.5f);
                                    }

                                    // Optionally show a message in the chat
                                    showSessionEndedMessage();
                                });
                            }
                            break; // Only need to handle the first session end message for this session
                        } else {
                            Log.d("ConnectChatActivity", "Ignoring old session end message: sender=" + senderId +
                                  ", timestamp=" + messageTimestamp + ", sessionStart=" + sessionStartTime);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ConnectChatActivity", "Error listening for session end messages", databaseError.toException());
            }
        });
    }

    /**
     * Show a message indicating the session has ended
     */
    private void showSessionEndedMessage() {
        // This message will be shown through the normal message flow via the adapter
        // The session end message sent by the other user will be displayed in the chat
    }

    /**
     * Gets the RecyclerView for scrolling to messages
     * @return The RecyclerView instance
     */
    public RecyclerView getRecyclerView() {
        return recyclerViewChat;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clear the active session ID when the activity is destroyed
        if (sessionId != null) {
            com.example.appdev.utils.ConnectionRequestManager.getInstance().clearActiveSessionId();
        }
    }
}
