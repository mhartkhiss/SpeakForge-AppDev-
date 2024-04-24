package com.example.appdev.adapter;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.models.ChatMessage;
import com.example.appdev.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;
    private DatabaseReference messagesRef;
    private String roomId;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public ChatAdapter(DatabaseReference messagesRef, String roomId) {
        this.messages = new ArrayList<>();
        this.messagesRef = messagesRef;
        this.roomId = roomId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the appropriate layout based on the senderId
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.item_message_sent : R.layout.item_message_received,
                parent, false);
        return new ChatViewHolder(view, messagesRef, roomId);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        // Determine the message type based on senderId
        String senderId = messages.get(position).getSenderId();
        return senderId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) ? 0 : 1;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewMessage;
        private DatabaseReference messagesRef;
        private String roomId;

        public ChatViewHolder(@NonNull View itemView, DatabaseReference messagesRef, String roomId) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            this.messagesRef = messagesRef;
            this.roomId = roomId;
        }

        public void bind(ChatMessage message) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
                if (message.getMessageOG() != null) {
                    textViewMessage.setText(message.getMessageOG());
                } else {
                    textViewMessage.setText(message.getMessage());
                }

                // Add OnClickListener to textViewMessage
                textViewMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Check if the messageOG is not null
                        if (message.getMessageOG() != null) {
                            // Toggle between original message and translated message
                            if (textViewMessage.getText().toString().equals(message.getMessage())) {
                                textViewMessage.setTypeface(null, Typeface.NORMAL);
                                textViewMessage.setText(message.getMessageOG());
                            } else {
                                textViewMessage.setTypeface(null, Typeface.ITALIC);
                                textViewMessage.setText(message.getMessage());
                            }
                        } else {
                            // Handle the case when messageOG is null
                            // Here you can show a message or take appropriate action
                        }
                    }
                });
            } else {
                textViewMessage.setText(message.getMessage());
                // Add OnClickListener to textViewMessage
                // Add OnClickListener to textViewMessage
                textViewMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Get the current message text
                        String currentMessage = textViewMessage.getText().toString();
                        String messageId = message.getMessageId();

                        // Create a list of message variations
                        List<String> messageVariations = new ArrayList<>();
                        messageVariations.add(message.getMessageVar1().replace("\"", ""));
                        messageVariations.add(message.getMessageVar2().replace("\"", ""));
                        messageVariations.add(message.getMessageVar3().replace("\"", ""));
                        //messageVariations.add(message.getMessageVar4().replace("\"", ""));
                        //messageVariations.add(message.getMessageVar5().replace("\"", ""));

                        //Toast.makeText(v.getContext(), roomId, Toast.LENGTH_SHORT).show();

                        // Find the index of the current message in the messageVariations list
                        int currentIndex = messageVariations.indexOf(currentMessage);

                        // Select the next variation in the list
                        String nextVariation;
                        if (currentIndex == messageVariations.size() - 1) {
                            // If the current message is the last variation in the list, select the first variation
                            nextVariation = messageVariations.get(0);
                        } else {
                            // Otherwise, select the next variation in the list
                            nextVariation = messageVariations.get(currentIndex + 1);
                        }

                        // Check if roomId and messageId are not null
                        if (roomId != null && messageId != null) {
                            // Update the message in the Firebase database
                            nextVariation = nextVariation.replace("\"", "");
                            messagesRef.child(roomId).child(messageId).child("message").setValue(nextVariation);
                        } else {
                            Log.e("ChatAdapter", "Room ID or Message ID is null");
                        }
                    }
                });
            }
        }


    }
}




