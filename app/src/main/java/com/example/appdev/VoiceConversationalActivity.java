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
import com.example.appdev.adapters.VoiceMessageAdapter;
import com.example.appdev.models.VoiceMessage;
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

public class VoiceConversationalActivity extends AppCompatActivity {

    private RecyclerView recyclerViewVoiceChat;
    private VoiceMessageAdapter voiceMessageAdapter;
    private DatabaseReference voiceMessagesRef;
    private String roomId, recipientLanguage;
    private static final int SPEECH_REQUEST_CODE = 1;
    private DatabaseReference contactSettingsRef;
    private boolean translateEnabled = false;
    private int previousMessageCount = 0;
    private String recipientTranslator = "google"; // default value
    private String recipientId;

    // Voice input UI elements
    private LinearLayout voiceInputContainer;
    private ImageButton buttonVoiceRecord;
    private TextView voiceStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add this line to adjust resize mode
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        setContentView(R.layout.activity_voice_conversational);

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
        voiceMessagesRef = database.getReference("voice_messages");

        // Initialize views
        recyclerViewVoiceChat = findViewById(R.id.recyclerViewVoiceChat);
        voiceInputContainer = findViewById(R.id.voiceInputContainer);
        buttonVoiceRecord = findViewById(R.id.buttonVoiceRecord);
        voiceStatusText = findViewById(R.id.voiceStatusText);

        // Initialize RecyclerView
        voiceMessageAdapter = new VoiceMessageAdapter(voiceMessagesRef, roomId);

        recyclerViewVoiceChat.setAdapter(voiceMessageAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewVoiceChat.setLayoutManager(layoutManager);
        recyclerViewVoiceChat.setAdapter(voiceMessageAdapter);

        // Set voice record button click listener
        buttonVoiceRecord.setOnClickListener(v -> startVoiceRecognition());

        // Back button
        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(v -> finish());

        loadVoiceMessages();

        DatabaseReference recipientLanguageRef = FirebaseDatabase.getInstance()
                .getReference("users").child(recipientId).child("language");
        recipientLanguageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recipientLanguage = dataSnapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        de.hdodenhof.circleimageview.CircleImageView imageViewUserPicture =
                findViewById(R.id.imageViewUserPicture);

        if (profileImageUrl != null && !profileImageUrl.equals("none")) {
            Glide.with(this)
                    .load(profileImageUrl)
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
                    Intent intent = new Intent(VoiceConversationalActivity.this, ContactSettingsActivity.class);
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
                        CustomNotification.showNotification(VoiceConversationalActivity.this,
                            "Failed to load user information", false);
                    }
                }
            });
        });

        // Initialize contact settings reference
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
                    translateEnabled = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                translateEnabled = false;
            }
        });
    }

    private String generateRoomId(String senderId, String recipientId) {
        String[] ids = {senderId, recipientId};
        Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }

    public void sendVoiceMessage(String voiceText, String targetLanguage) {
        if (voiceText.trim().isEmpty()) {
            return;
        }

        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String messageId = voiceMessagesRef.child(roomId).push().getKey();
        long timestamp = System.currentTimeMillis();

        // Create voice message data
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageId);
        messageData.put("voiceText", voiceText);
        messageData.put("timestamp", timestamp);
        messageData.put("senderId", senderId);
        messageData.put("senderLanguage", Variables.userLanguage);
        messageData.put("translationMode", Variables.isFormalTranslationMode ? "formal" : "casual");
        messageData.put("translationState", "TRANSLATING");

        // Save the message to Firebase
        if (messageId != null) {
            voiceMessagesRef.child(roomId).child(messageId).setValue(messageData)
                .addOnSuccessListener(aVoid -> {
                    // Message saved successfully, now translate it
                    translateVoiceMessage(targetLanguage, voiceText, messageId);
                })
                .addOnFailureListener(e -> {
                    Log.e("VoiceConversationalActivity", "Failed to save voice message: " + e.getMessage());
                });
        }
    }

    private void translateVoiceMessage(String targetLanguage, String voiceText, String messageId) {
        Variables.openAiPrompt = 1; // Use standard translation setting

        // Prepare the request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("text", voiceText);
            requestBody.put("source_language", Variables.userLanguage);
            requestBody.put("target_language", targetLanguage);
            requestBody.put("mode", Variables.isFormalTranslationMode ? "formal" : "casual");
            requestBody.put("variants", "single");
            requestBody.put("translator", recipientTranslator);
            requestBody.put("room_id", roomId);
            requestBody.put("message_id", messageId);
            requestBody.put("update_state", true);

            String apiUrl = Variables.API_TRANSLATE_VOICE_URL;

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
                            return true;
                        } else {
                            // If translation fails, set state back to null
                            DatabaseReference messageRef = voiceMessagesRef.child(roomId).child(messageId);
                            messageRef.child("translationState").setValue(null);
                            return false;
                        }
                    } catch (Exception e) {
                        Log.e("VoiceConversationalActivity", "Error translating voice message: " + e.getMessage());
                        DatabaseReference messageRef = voiceMessagesRef.child(roomId).child(messageId);
                        messageRef.child("translationState").setValue(null);
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean success) {
                    if (!success) {
                        Log.e("VoiceConversationalActivity", "Failed to translate voice message");
                    }
                }
            }.execute();
        } catch (Exception e) {
            Log.e("VoiceConversationalActivity", "Error creating JSON request: " + e.getMessage());
            DatabaseReference messageRef = voiceMessagesRef.child(roomId).child(messageId);
            messageRef.child("translationState").setValue(null);
        }
    }

    private void loadVoiceMessages() {
        if (roomId != null) {
            voiceMessagesRef.child(roomId).orderByChild("timestamp")
                    .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<VoiceMessage> messages = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        VoiceMessage message = snapshot.getValue(VoiceMessage.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    voiceMessageAdapter.setMessages(messages);

                    // Only scroll if new messages are added
                    int newSize = messages.size();
                    if (newSize > previousMessageCount) {
                        recyclerViewVoiceChat.scrollToPosition(voiceMessageAdapter.getItemCount() - 1);
                    }
                    previousMessageCount = newSize;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e("VoiceConversationalActivity", "Error loading voice messages: " + databaseError.getMessage());
                }
            });
        }
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
            voiceStatusText.setText("Listening...");
        } catch (Exception e) {
            CustomNotification.showNotification(this,
                "Speech recognition not available on this device", false);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == this.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                voiceStatusText.setText("Processing...");
                sendVoiceMessage(spokenText, recipientLanguage);
                voiceStatusText.setText("Tap microphone to speak");
            }
        }
    }

    /**
     * Gets the RecyclerView for scrolling to messages
     * @return The RecyclerView instance
     */
    public RecyclerView getRecyclerView() {
        return recyclerViewVoiceChat;
    }
}
