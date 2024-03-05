package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.example.appdev.adapter.ChatAdapter;
import com.example.appdev.classes.FetchUserField;
import com.example.appdev.classes.Translate;
import com.example.appdev.classes.TranslationTask;
import com.example.appdev.models.ChatMessage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class ConversationModeActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ChatAdapter chatAdapter;
    private DatabaseReference messagesRef;
    private TextView textViewRecipient;
    private TextView textViewTranslate;
    private String roomId, recipientLanguage, senderLanguage;
    private Translate translate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_mode);

        // Retrieve recipient information from intent extras
        String recipientId = getIntent().getStringExtra("userId");

        // Generate a unique room ID for the conversation using the sender and recipient IDs
        String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        roomId = generateRoomId(senderId, recipientId);

        // Initialize Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("messages");

        // Initialize views
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);

        // Initialize RecyclerView
        chatAdapter = new ChatAdapter();

        recyclerViewChat.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);


        // Set click listener for send button
        buttonSend.setOnClickListener(v -> sendMessage());
        loadMessages();
        textViewRecipient = findViewById(R.id.textViewRecipient);

        // Retrieve recipient information from intent extras
        String recipientName = getIntent().getStringExtra("username");
        String recipientEmail = getIntent().getStringExtra("email");
        recipientLanguage = getIntent().getStringExtra("recipientLanguage");
        System.out.println("Recipient Language: " + recipientLanguage);


        // Set recipient name or email to the textViewRecipient
        if (recipientName != null && !recipientName.isEmpty()) {
            textViewRecipient.setText(recipientName);
        } else if (recipientEmail != null && !recipientEmail.isEmpty()) {
            textViewRecipient.setText(recipientEmail);
        }
    }

    private String generateRoomId(String senderId, String recipientId) {
        // Sort sender and recipient IDs alphabetically to ensure consistency
        String[] ids = {senderId, recipientId};
        Arrays.sort(ids);

        // Concatenate sender and recipient IDs to create the room ID
        return ids[0] + "_" + ids[1];
    }
    private void sendMessage() {
        // Get the sender and target languages
        FetchUserField.fetchUserField("sourceLanguage", new FetchUserField.UserFieldListener() {
            @Override
            public void onFieldReceived(String senderLanguage) {
                String targetLanguage = recipientLanguage;

                // Send the message directly
                sendDirectMessage(senderLanguage, targetLanguage);
            }

            @Override
            public void onError(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void sendDirectMessage(String senderLanguage, String targetLanguage) {
        String messageTextOG = editTextMessage.getText().toString().trim();
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
            messageData.put("senderId", senderId); // Add sender ID to message data

            // Save message to Firebase Database
            messagesRef.child(roomId).child(messageId).setValue(messageData)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Translate the message after it is sent
                                String messageTextOG2 = "\"" + messageTextOG + "\"";
                                translateTextAndSendMessage(senderLanguage, targetLanguage, messageTextOG2, messageId);
                            } else {
                                Log.e("ConversationModeActivity", "Failed to send message: " + task.getException());
                            }
                        }
                    });

            // Clear the input field
            editTextMessage.setText("");
        } else {
            Log.e("ConversationModeActivity", "Sender ID or Room ID is null");
        }
    }

    private void translateTextAndSendMessage(String senderLanguage, String targetLanguage, String messageTextOG, String messageId) {
        TranslationTask translationTask = new TranslationTask(targetLanguage, new TranslationTask.TranslationListener() {
            @Override
            public void onTranslationComplete(String translatedMessage) {
                if (!TextUtils.isEmpty(translatedMessage)) {
                    updateMessageWithTranslation(messageId, translatedMessage);
                }
            }
        });
        translationTask.execute(messageTextOG);
    }



    private void updateMessageWithTranslation(String messageId, String translatedMessage) {
        String roomId = ConversationModeActivity.this.roomId;

        // Check if the translated message starts and ends with double quotation marks
        if (translatedMessage.startsWith("\"") && translatedMessage.endsWith("\"")) {
            // Remove the double quotation marks
            translatedMessage = translatedMessage.substring(1, translatedMessage.length() - 1);
        }

        // Update the message with translated text
        messagesRef.child(roomId).child(messageId).child("message").setValue(translatedMessage);
    }





    private void loadMessages() {
        String roomId = this.roomId;
        if (roomId != null) {
            messagesRef.child(roomId).orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<ChatMessage> messages = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        ChatMessage message = snapshot.getValue(ChatMessage.class);
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




}

