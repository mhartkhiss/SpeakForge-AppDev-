package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapter.ChatAdapter;
import com.example.appdev.tasks.FetchUserField;
import com.example.appdev.tasks.Translation;
import com.example.appdev.models.Message;
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
    private EditText chatBox;
    private ImageButton buttonSend;
    private ChatAdapter chatAdapter;
    private DatabaseReference messagesRef;
    private String roomId, recipientLanguage;


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
        //layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(chatAdapter);


        // Set click listener for send button
        buttonSend.setOnClickListener(v -> sendMessage());

        ImageButton buttonMic = findViewById(R.id.buttonMic);
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
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadMessages();
        //textViewRecipient = findViewById(R.id.textViewRecipient);

        // Retrieve recipient information from intent extras
        String recipientName = getIntent().getStringExtra("username");
        String recipientEmail = getIntent().getStringExtra("email");
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
        TextView textViewRecipientLanguage = findViewById(R.id.textViewRecipientLanguage);

        textViewUsername.setText(recipientName);
        textViewRecipientLanguage.setText(recipientLanguage);
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
        FetchUserField.fetchUserField("language", new FetchUserField.UserFieldListener() {
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
        String messageTextOG = chatBox.getText().toString().trim();
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
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Translate the message after it is sent
                                String messageTextOG2 = "\"" + messageTextOG + "\"";
                                if(targetLanguage != null){
                                    translateTextAndSendMessage(senderLanguage, targetLanguage, messageTextOG2, messageId);
                                }
                                else{
                                    messagesRef.child(roomId).child(messageId).child("message").setValue(messageTextOG);
                                }
                            } else {
                                Log.e("ConversationModeActivity", "Failed to send message: " + task.getException());
                            }
                        }
                    });

            // Clear the input field
            chatBox.setText("");
        } else {
            Log.e("ConversationModeActivity", "Sender ID or Room ID is null");
        }
    }

    private void translateTextAndSendMessage(String senderLanguage, String targetLanguage, String messageTextOG, String messageId) {
        Translation translationTask = new Translation(targetLanguage, new Translation.TranslationListener() {
            @Override
            public void onTranslationComplete(String translatedMessage) {
                if (!TextUtils.isEmpty(translatedMessage)) {
                    //updateMessageWithTranslation(messageId, translatedMessage);
                    storeTranslatedText(translatedMessage, messageId);
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

    private void storeTranslatedText(String translatedMessage, String messageId) {
        // Split the translated text by line breaks
        String[] lines = translatedMessage.split("\n");

        // Check if there are at least 5 lines
        if (lines.length >= 3) {
            // Store each line in a separate variable
            String messageVar1 = lines[0];
            String messageVar2 = lines[1];
            String messageVar3 = lines[2];

            messageVar1 = removeQuotationMarks(messageVar1);
            messageVar2 = removeQuotationMarks(messageVar2);
            messageVar3 = removeQuotationMarks(messageVar3);

            messageVar1 = messageVar1.replaceFirst("^\\d+\\.\\s*", "");
            messageVar2 = messageVar2.replaceFirst("^\\d+\\.\\s*", "");
            messageVar3 = messageVar3.replaceFirst("^\\d+\\.\\s*", "");

            // Update the message with translated text
            messagesRef.child(roomId).child(messageId).child("messageVar1").setValue(messageVar1);
            messagesRef.child(roomId).child(messageId).child("messageVar2").setValue(messageVar2);
            messagesRef.child(roomId).child(messageId).child("messageVar3").setValue(messageVar3);

            updateMessageWithTranslation(messageId, messageVar1);
        } else {
            Log.e("ConversationModeActivity", "Translated text does not have at least 5 lines");
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




}

