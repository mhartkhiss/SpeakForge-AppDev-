package com.example.appdev.adapters;

import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.models.Message;
import com.example.appdev.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
        private de.hdodenhof.circleimageview.CircleImageView imageViewProfile;
        private DatabaseReference usersRef;

        public ChatViewHolder(@NonNull View itemView, DatabaseReference messagesRef, String roomId, List<TextView> visibleOriginalMessages) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            this.messagesRef = messagesRef;
            this.roomId = roomId;
            this.visibleOriginalMessages = visibleOriginalMessages;
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            usersRef = FirebaseDatabase.getInstance().getReference("users");
        }

        public void bind(Message message) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
                loadProfileImage(currentUser.getUid());
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
                loadProfileImage(message.getSenderId());
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
                        if (message.getMessageVar1() != null) {
                            messageVariations.add(message.getMessageVar1().replace("\"", ""));
                        }
                        if (message.getMessageVar2() != null) {
                            messageVariations.add(message.getMessageVar2().replace("\"", ""));
                        }
                        if (message.getMessageVar3() != null) {
                            messageVariations.add(message.getMessageVar3().replace("\"", ""));
                        }

                        if (!messageVariations.isEmpty()) {
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
                    }
                });
            }

        }
        private void loadProfileImage(String senderId) {
            usersRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        // Check if the layout is item_message_sent or item_message_received
                        int layoutType = getItemViewType();
                        if (layoutType == 0) { // If layout is item_message_sent, don't load image
                            return;
                        } else if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            // Load profile image if URL is not null or empty
                            Glide.with(itemView.getContext())
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.default_userpic)
                                    .into(imageViewProfile);
                        } else {
                            // Use a default image if profileImageUrl is null or empty
                            imageViewProfile.setImageResource(R.drawable.default_userpic);
                        }
                    } else {
                        // Use a default image if user data doesn't exist
                        imageViewProfile.setImageResource(R.drawable.default_userpic);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Use a default image if database operation is cancelled
                    imageViewProfile.setImageResource(R.drawable.default_userpic);
                }
            });
        }
    }
}
