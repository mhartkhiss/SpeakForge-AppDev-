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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.example.appdev.adapters.ChatAdapter;
import com.example.appdev.translators.Translation_GoogleTranslate;
import com.example.appdev.translators.Translation_OpenAI;
import com.example.appdev.models.Message;
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

public class ConversationModeActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText chatBox;
    private ImageButton buttonSend;
    private ChatAdapter chatAdapter;
    private DatabaseReference messagesRef;
    private String roomId, recipientLanguage;
    private static final int SPEECH_REQUEST_CODE = 1;


    //Establish Connection
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_mode);

        // Retrieve recipient information from intent extras
        String recipientId = getIntent().getStringExtra("userId");

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
        chatAdapter = new ChatAdapter(messagesRef, roomId);

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
                TextView textViewRecipientLanguage = findViewById(R.id.textViewRecipientLanguage);
                textViewRecipientLanguage.setText(recipientLanguage);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        // Retrieve recipient information from intent extras
        String recipientName = getIntent().getStringExtra("username");
        recipientLanguage = getIntent().getStringExtra("recipientLanguage");
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");
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
            if(targetLanguage == null){
                messageData.put("message", messageTextOG);;
            }
            else {
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
                            // Translate the message after it is sent
                            String messageTextOG2 = "\"" + messageTextOG + "\"";
                            if(targetLanguage != null){
                                translateMessage(targetLanguage, messageTextOG2, messageId);
                            }
                            else{
                                messagesRef.child(roomId).child(messageId).child("message").setValue(messageTextOG);
                            }
                        } else {
                            Log.e("ConversationModeActivity", "Failed to send message: " + task.getException());
                        }
                    });

            // Clear the input field
            chatBox.setText("");
        } else {
            Log.e("ConversationModeActivity", "Sender ID or Room ID is null");
        }
    }

    private void translateMessage(String targetLanguage, String messageTextOG, String messageId) {

        if(Variables.userTranslator.equals("openai")){
            // OpenAI
            Variables.openAiPrompt = 1;
            Translation_OpenAI translationOpenAITask = new Translation_OpenAI(targetLanguage, translatedMessage -> {
                if (!TextUtils.isEmpty(translatedMessage)) {
                    //storeTranslatedText(translatedMessage, messageId);
                    messagesRef.child(roomId).child(messageId).child("message").setValue(removeQuotationMarks(translatedMessage));
                }
            });
            translationOpenAITask.execute(messageTextOG);
        } else {
            // Google Translate
            Translation_GoogleTranslate translationGoogleTask = new Translation_GoogleTranslate(ConversationModeActivity.this);
            translationGoogleTask.translateText(messageTextOG, targetLanguage, new Translation_GoogleTranslate.TranslateListener() {
                @Override
                public void onSuccess(String translatedText) {
                    if (!TextUtils.isEmpty(translatedText)) {
                        messagesRef.child(roomId).child(messageId).child("message").setValue(
                                removeQuotationMarks(translatedText));
                    }
                }

                @Override
                public void onError(VolleyError error) {
                    // Handle error
                }
            });
        }
    }

    private String removeQuotationMarks(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
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
                    recyclerViewChat.scrollToPosition(chatAdapter.getItemCount() - 1);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                    Log.e("ConversationModeActivity", "Error loading messages: " + databaseError.getMessage());
                }
            });
        } else {
            Log.e("ConversationModeActivity", "Room ID is null");
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

