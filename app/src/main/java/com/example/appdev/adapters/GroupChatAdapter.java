package com.example.appdev.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.R;
import com.example.appdev.models.GroupMessage;
import com.example.appdev.utils.LoadingDotsView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder> {

    private List<GroupMessage> messages;
    private DatabaseReference messagesRef;
    private String groupId;
    private Context context;

    public GroupChatAdapter() {
        this.messages = new ArrayList<>();
    }

    public GroupChatAdapter(DatabaseReference messagesRef, String groupId, Context context) {
        this.messagesRef = messagesRef;
        this.groupId = groupId;
        this.context = context;
        messages = new ArrayList<>();
    }

    public void setMessages(List<GroupMessage> messages) {
        if (messages == null) {
            this.messages = new ArrayList<>();
        } else {
            // Filter out any null messages
            this.messages = messages.stream()
                .filter(message -> message != null && message.getSenderId() != null)
                .collect(Collectors.toList());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.item_group_message_sent : R.layout.item_group_message_received,
                parent, false);
        return new GroupChatViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupChatViewHolder holder, int position) {
        GroupMessage message = messages.get(position);
        
        // Check if this message is part of consecutive messages from same sender
        boolean showSenderInfo = true;
        if (position > 0) {
            GroupMessage previousMessage = messages.get(position - 1);
            if (message.getSenderId().equals(previousMessage.getSenderId())) {
                showSenderInfo = false;
            }
        }
        
        holder.bind(message, showSenderInfo);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        GroupMessage message = messages.get(position);
        if (message == null || message.getSenderId() == null || 
            FirebaseAuth.getInstance().getCurrentUser() == null) {
            return 1; // Default to received message layout if any value is null
        }
        String senderId = message.getSenderId();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return senderId.equals(currentUserId) ? 0 : 1;
    }

    public static class GroupChatViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewMessage, textViewOriginalMessage, textViewSenderName;
        private LoadingDotsView loadingDots;
        private de.hdodenhof.circleimageview.CircleImageView imageViewProfile;
        private Context context;
        private CardView messageCard;

        public GroupChatViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            loadingDots = itemView.findViewById(R.id.loadingDots);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            this.context = context;
            messageCard = itemView.findViewById(R.id.cardMessage);
        }

        public void bind(GroupMessage message, boolean showSenderInfo) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            // Handle sender's profile image
            if (message.getSenderProfileUrl() != null && !message.getSenderProfileUrl().isEmpty()) {
                if (imageViewProfile != null) {
                    imageViewProfile.setVisibility(showSenderInfo ? View.VISIBLE : View.INVISIBLE);
                    if (showSenderInfo) {
                        Glide.with(context)
                                .load(message.getSenderProfileUrl())
                                .placeholder(R.drawable.default_userpic)
                                .into(imageViewProfile);
                    }
                }
            } else if (imageViewProfile != null) {
                imageViewProfile.setVisibility(showSenderInfo ? View.VISIBLE : View.INVISIBLE);
                if (showSenderInfo) {
                    imageViewProfile.setImageResource(R.drawable.default_userpic);
                }
            }

            // Show sender name for received messages
            if (textViewSenderName != null) {
                textViewSenderName.setVisibility(showSenderInfo ? View.VISIBLE : View.GONE);
                if (showSenderInfo && message.getSenderName() != null) {
                    textViewSenderName.setText(message.getSenderName());
                }
            }

            if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
                // Sent messages
                if (message.getMessageOG() != null) {
                    textViewMessage.setText(message.getMessageOG());
                } else {
                    textViewMessage.setText(message.getMessage());
                }

                // Allow toggling between original and translated message
                textViewMessage.setOnClickListener(v -> {
                    if (message.getMessageOG() != null && !message.getMessageOG().equals(message.getMessage())) {
                        if (textViewMessage.getText().toString().equals(message.getMessage())) {
                            textViewMessage.setTypeface(null, Typeface.NORMAL);
                            textViewMessage.setText(message.getMessageOG());
                        } else {
                            textViewMessage.setTypeface(null, Typeface.ITALIC);
                            textViewMessage.setText(message.getMessage());
                        }
                    }
                });
            } else {
                // Received messages
                // Handle loading state
                if (message.getMessage() != null && message.getMessage().equals("......")) {
                    textViewMessage.setVisibility(View.GONE);
                    loadingDots.setVisibility(View.VISIBLE);
                    loadingDots.startAnimation();
                } else {
                    textViewMessage.setVisibility(View.VISIBLE);
                    loadingDots.setVisibility(View.GONE);
                    loadingDots.stopAnimation();
                    textViewMessage.setText(message.getMessage());
                }

                // Change background color based on translation status
                if (messageCard != null) {
                    if (message.getMessage() != null && 
                        message.getMessageOG() != null && 
                        message.getMessage().equals(message.getMessageOG())) {
                        // Message is not translated - use light gray
                        messageCard.setCardBackgroundColor(context
                            .getResources().getColor(R.color.light_gray));
                    } else {
                        // Message is translated - use original orange color
                        messageCard.setCardBackgroundColor(context
                            .getResources().getColor(R.color.message_received_bg));
                    }
                }
            }
        }
    }
}
