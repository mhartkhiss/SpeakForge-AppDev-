package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.example.appdev.adapters.ChatAdapter;
import com.example.appdev.translators.Translation_GoogleTranslate;
import com.example.appdev.translators.Translation_OpenAI;
import com.example.appdev.translators.Translation_DeepSeekV3;
import com.example.appdev.translators.Translation_GPT4;
import com.example.appdev.translators.Translation_Gemini;
import com.example.appdev.translators.Translation_Claude;
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
import com.example.appdev.translators.TranslatorFactory;
import com.example.appdev.translators.TranslatorType;

public class ConversationModeActivity extends AppCompatActivity {

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


    //Establish Connection
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Add this line to adjust resize mode
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        setContentView(R.layout.activity_conversation_mode);

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
                    Intent intent = new Intent(ConversationModeActivity.this, ContactSettingsActivity.class);
                    intent.putExtra("username", recipientName);
                    intent.putExtra("email", recipientEmail);
                    intent.putExtra("language", recipientLanguage);
                    intent.putExtra("profileImageUrl", profileImageUrl);
                    intent.putExtra("userId", recipientId);
                    startActivity(intent);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    CustomNotification.showNotification(ConversationModeActivity.this, 
                        "Failed to load user information", false);
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
        String messageTextOG = message;
        String roomId = ConversationModeActivity.this.roomId;

        // Get the current user ID (sender ID)
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Check if senderId and roomId are not null
        if (senderId != null && roomId != null) {
            // Create a unique key for the message
            String messageId = messagesRef.child(roomId).push().getKey();

            // Get current timestamp
            long timestamp = System.currentTimeMillis();

            // Create a HashMap to represent the message data
            HashMap<String, Object> messageData = new HashMap<>();
            
            // Set initial message value based on translation setting
            if (targetLanguage == null || !translateEnabled) {
                messageData.put("message", messageTextOG);
            } else {
                messageData.put("message", "......");
            }
            
            messageData.put("messageOG", messageTextOG);
            messageData.put("timestamp", timestamp);
            messageData.put("senderId", senderId);
            messageData.put("messageId", messageId);

            // Save message to Firebase Database
            messagesRef.child(roomId).child(messageId).setValue(messageData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String messageTextOG2 = "\"" + messageTextOG + "\"";
                            // Only translate if translation is enabled and target language exists
                            if (targetLanguage != null && translateEnabled) {
                                translateMessage(targetLanguage, messageTextOG2, messageId);
                            } else {
                                // If translation is disabled, use original message
                                messagesRef.child(roomId).child(messageId)
                                    .child("message").setValue(messageTextOG);
                            }
                        } else {
                            Log.e("ConversationModeActivity", 
                                "Failed to send message: " + task.getException());
                        }
                    });

            // Clear the input field
            chatBox.setText("");
        } else {
            Log.e("ConversationModeActivity", "Sender ID or Room ID is null");
        }
    }

    private void translateMessage(String targetLanguage, String messageTextOG, String messageId) {
        // Set to single translation mode (not variations)
        Variables.openAiPrompt = 1;

        AsyncTask<String, Void, String> translator = TranslatorFactory.createTranslator(
            TranslatorType.fromId(recipientTranslator),
            targetLanguage,
            translatedMessage -> {
                if (!TextUtils.isEmpty(translatedMessage)) {
                    String cleanTranslation = removeQuotationMarks(translatedMessage);
                    messagesRef.child(roomId).child(messageId).child("message")
                        .setValue(cleanTranslation);
                }
            },
            this
        );
        translator.execute(messageTextOG);
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


}

