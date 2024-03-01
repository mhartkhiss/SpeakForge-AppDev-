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

import com.example.appdev.adapter.ChatAdapter;
import com.example.appdev.models.ChatMessage;
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
    private String roomId;


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

        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);

        // Set click listener for send button
        buttonSend.setOnClickListener(v -> sendMessage());
        loadMessages();
        textViewRecipient = findViewById(R.id.textViewRecipient);

        // Retrieve recipient information from intent extras
        String recipientName = getIntent().getStringExtra("username");
        String recipientEmail = getIntent().getStringExtra("email");

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
        String messageText = editTextMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(messageText)) {
            // Get the current user ID (sender ID)
            String senderId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Get the room ID
            String roomId = this.roomId;

            // Check if senderId and roomId are not null
            if (senderId != null && roomId != null) {
                // Create a unique key for the message
                String messageId = messagesRef.child(roomId).push().getKey();

                // Get current timestamp
                long timestamp = System.currentTimeMillis();

                // Create a HashMap to represent the message data
                HashMap<String, Object> messageData = new HashMap<>();
                messageData.put("message", messageText);
                messageData.put("timestamp", timestamp);
                messageData.put("senderId", senderId); // Add sender ID to message data

                // Save message to Firebase Database
                messagesRef.child(roomId).child(messageId).setValue(messageData);

                // Clear the input field
                editTextMessage.setText("");
            } else {
                Log.e("ConversationModeActivity", "Sender ID or Room ID is null");
            }
        }
    }

    private void loadMessages() {
        String roomId = this.roomId;
        if (roomId != null) {
            messagesRef.child(roomId).addValueEventListener(new ValueEventListener() {
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

