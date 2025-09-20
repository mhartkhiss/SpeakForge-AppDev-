package com.example.appdev.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.ChatActivity;
import com.example.appdev.GroupChatActivity;
import com.example.appdev.R;
import com.example.appdev.models.ChatItem;

import java.util.List;

public class ChatItemAdapter extends RecyclerView.Adapter<ChatItemAdapter.ChatItemViewHolder> {

    private final List<ChatItem> chatItems;
    private final Context context;
    private final String currentUserId;
    private final OnChatItemMoreClickListener moreClickListener;

    public interface OnChatItemMoreClickListener {
        void onMoreClick(View view, ChatItem chatItem);
    }

    public ChatItemAdapter(List<ChatItem> chatItems, Context context, String currentUserId, OnChatItemMoreClickListener listener) {
        this.chatItems = chatItems;
        this.context = context;
        this.currentUserId = currentUserId;
        this.moreClickListener = listener;
    }

    @NonNull
    @Override
    public ChatItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ChatItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatItemViewHolder holder, int position) {
        ChatItem chatItem = chatItems.get(position);
        holder.textViewUsername.setText(chatItem.getName());
        
        // Show group icon indicator for group chats
        holder.imageViewGroupIndicator.setVisibility(chatItem.isGroup() ? View.VISIBLE : View.GONE);
        
        // Show "Group" label for group chats
        holder.textViewGroupLabel.setVisibility(chatItem.isGroup() ? View.VISIBLE : View.GONE);
        
        // Format last message
        if (chatItem.getLastMessage() != null && !chatItem.getLastMessage().isEmpty()) {
            String displayMessage;
            
            if (chatItem.isGroup()) {
                // For group chats, handle the last message display
                boolean isOwnMessage = chatItem.getLastMessageSenderId() != null && 
                                      chatItem.getLastMessageSenderId().equals(currentUserId);
                
                // Just use the message text directly, without trying to use messageOG
                displayMessage = isOwnMessage ? 
                    "You: " + (chatItem.getLastMessage() != null ? chatItem.getLastMessage() : "") : 
                    chatItem.getLastMessage() != null ? chatItem.getLastMessage() : "";
                
                holder.textViewEmail.setText(displayMessage);
                holder.textViewEmail.setTextColor(context.getResources().getColor(R.color.grey));
                holder.textViewEmail.setTypeface(null, isOwnMessage ? Typeface.BOLD : Typeface.NORMAL);
            } else {
                // For direct chats, handle the last message display
                boolean isOwnMessage = chatItem.getLastMessageSenderId() != null && 
                                        chatItem.getLastMessageSenderId().equals(currentUserId);
                
                // Just use the message text directly, without trying to use messageOG
                displayMessage = isOwnMessage ? 
                    "You: " + (chatItem.getLastMessage() != null ? chatItem.getLastMessage() : "") : 
                    chatItem.getLastMessage() != null ? chatItem.getLastMessage() : "";
                
                holder.textViewEmail.setText(displayMessage);
                holder.textViewEmail.setTextColor(context.getResources().getColor(R.color.grey));
                holder.textViewEmail.setTypeface(null, isOwnMessage ? Typeface.BOLD : Typeface.NORMAL);
            }
        } else {
            // No message yet
            holder.textViewEmail.setText(chatItem.isGroup() ? "No messages yet" : "No conversation yet");
            holder.textViewEmail.setTextColor(context.getResources().getColor(R.color.grey));
            holder.textViewEmail.setTypeface(null, Typeface.ITALIC);
        }

        // Load profile/group image
        if (chatItem.getImageUrl() != null && !chatItem.getImageUrl().isEmpty() && !chatItem.getImageUrl().equals("none")) {
            Glide.with(context)
                 .load(chatItem.getImageUrl())
                 .placeholder(chatItem.isGroup() ? R.drawable.group_default_icon : R.drawable.default_userpic)
                 .into(holder.imageViewProfilePic);
        } else {
            holder.imageViewProfilePic.setImageResource(
                chatItem.isGroup() ? R.drawable.group_default_icon : R.drawable.default_userpic);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> openChat(chatItem));
        holder.buttonMore.setOnClickListener(v -> moreClickListener.onMoreClick(v, chatItem));
    }
    
    private void openChat(ChatItem chatItem) {
        if (chatItem.isGroup()) {
            // Open group chat
            Intent intent = new Intent(context, GroupChatActivity.class);
            intent.putExtra("groupId", chatItem.getId());
            intent.putExtra("groupName", chatItem.getName());
            intent.putExtra("groupImageUrl", chatItem.getImageUrl());
            context.startActivity(intent);
        } else {
            // Open direct chat - temporarily disabled
            // Intent intent = new Intent(context, ChatActivity.class);
            // intent.putExtra("userId", chatItem.getId());
            // intent.putExtra("username", chatItem.getName());
            // intent.putExtra("profileImageUrl", chatItem.getImageUrl());
            // context.startActivity(intent);
        }
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    static class ChatItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewUsername;
        private final TextView textViewEmail;
        private final TextView textViewGroupLabel;
        private final ImageView imageViewProfilePic;
        private final ImageView buttonMore;
        private final ImageView imageViewGroupIndicator;

        public ChatItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewGroupLabel = itemView.findViewById(R.id.textViewGroupLabel);
            imageViewProfilePic = itemView.findViewById(R.id.imageViewUserPicture);
            buttonMore = itemView.findViewById(R.id.buttonMore);
            imageViewGroupIndicator = itemView.findViewById(R.id.imageViewGroupIndicator);
        }
    }
}
