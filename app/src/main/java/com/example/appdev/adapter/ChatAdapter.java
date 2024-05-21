package com.example.appdev.adapter;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.models.Message;
import com.example.appdev.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messages;
    private DatabaseReference messagesRef;
    private String roomId;
    private List<TextView> visibleOriginalMessages;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
        this.visibleOriginalMessages = new ArrayList<>();
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public ChatAdapter(DatabaseReference messagesRef, String roomId) {
        this.messages = new ArrayList<>();
        this.messagesRef = messagesRef;
        this.roomId = roomId;
        this.visibleOriginalMessages = new ArrayList<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.item_message_sent : R.layout.item_message_received,
                parent, false);
        return new ChatViewHolder(view, messagesRef, roomId, visibleOriginalMessages);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        String senderId = messages.get(position).getSenderId();
        return senderId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) ? 0 : 1;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewMessage, textViewOriginalMessage;
        private DatabaseReference messagesRef;
        private String roomId;
        private List<TextView> visibleOriginalMessages;

        public ChatViewHolder(@NonNull View itemView, DatabaseReference messagesRef, String roomId, List<TextView> visibleOriginalMessages) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            this.messagesRef = messagesRef;
            this.roomId = roomId;
            this.visibleOriginalMessages = visibleOriginalMessages;
        }

        public void bind(Message message) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
                if (message.getMessageOG() != null) {
                    textViewMessage.setText(message.getMessageOG());
                } else {
                    textViewMessage.setText(message.getMessage());
                }

                textViewMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (message.getMessageOG() != null) {
                            // Toggle between original message and translated message
                            if (textViewMessage.getText().toString().equals(message.getMessage())) {
                                textViewMessage.setTypeface(null, Typeface.NORMAL);
                                textViewMessage.setText(message.getMessageOG());
                            } else {
                                textViewMessage.setTypeface(null, Typeface.ITALIC);
                                textViewMessage.setText(message.getMessage());
                            }
                        }
                    }
                });
            } else {
                textViewMessage.setText(message.getMessage());
                textViewMessage.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        // Hide all visible original messages
                        for (TextView textView : visibleOriginalMessages) {
                            textView.setVisibility(View.GONE);
                        }
                        visibleOriginalMessages.clear();

                        textViewOriginalMessage.setVisibility(View.VISIBLE);
                        textViewOriginalMessage.setText(message.getMessageOG());
                        visibleOriginalMessages.add(textViewOriginalMessage);
                        return true;
                    }
                });
                textViewMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Get the current message text
                        String currentMessage = textViewMessage.getText().toString();
                        String messageId = message.getMessageId();

                        for (TextView textView : visibleOriginalMessages) {
                            textView.setVisibility(View.GONE);
                        }
                        visibleOriginalMessages.clear();

                        List<String> messageVariations = new ArrayList<>();
                        messageVariations.add(message.getMessageVar1().replace("\"", ""));
                        messageVariations.add(message.getMessageVar2().replace("\"", ""));
                        messageVariations.add(message.getMessageVar3().replace("\"", ""));

                        int currentIndex = messageVariations.indexOf(currentMessage);

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
