package com.example.appdev.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.ChatActivity;
import com.example.appdev.R;
import com.example.appdev.models.User;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Arrays;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> userList;
    private final Context context;
    private final String currentUserId;
    private final OnMoreButtonClickListener moreButtonClickListener;

    public interface OnMoreButtonClickListener {
        void onMoreButtonClick(View view, User user);
    }

    public UserAdapter(List<User> userList, Context context, String currentUserId, OnMoreButtonClickListener listener) {
        this.userList = userList;
        this.context = context;
        this.currentUserId = currentUserId;
        this.moreButtonClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewUsername.setText(user.getUsername());
        
        // Show last message if exists, otherwise show email
        if (user.getLastMessage() != null && !user.getLastMessage().isEmpty()) {
            String[] messageParts = user.getLastMessage().split("\\|", 3);
            if (messageParts.length == 3) {
                String senderId = messageParts[0];
                String messageText = messageParts[1];
                String messageOG = messageParts[2];
                
                // Use messageOG for own messages, translated message for others
                String displayMessage;
                if (senderId.equals(currentUserId)) {
                    displayMessage = "You: " + messageOG;
                } else {
                    displayMessage = messageText;
                }
                
                holder.textViewEmail.setText(displayMessage);
                holder.textViewEmail.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.grey));
                holder.textViewEmail.setTypeface(null, 
                    senderId.equals(currentUserId) ? Typeface.BOLD : Typeface.NORMAL);
            }
        } else {
            holder.textViewEmail.setText(user.getEmail());
            holder.textViewEmail.setTextColor(holder.itemView.getContext()
                .getResources().getColor(R.color.grey));
            holder.textViewEmail.setTypeface(null, Typeface.ITALIC);
        }

        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none")) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.default_userpic)
                    .into(holder.imageViewUserPicture);
        } else {
            holder.imageViewUserPicture.setImageResource(R.drawable.default_userpic);
        }

        holder.itemView.setOnClickListener(v -> {
            // Chat feature temporarily disabled
            // Intent intent = new Intent(context, ChatActivity.class);
            // intent.putExtra("userId", user.getUserId());
            // intent.putExtra("username", user.getUsername());
            // intent.putExtra("recipientLanguage", user.getLanguage());
            // intent.putExtra("profileImageUrl", user.getProfileImageUrl());
            // context.startActivity(intent);
        });

        // Set click listener for more button
        holder.buttonMore.setOnClickListener(v -> 
            moreButtonClickListener.onMoreButtonClick(v, user));
    }

    private void showPopupMenu(View view, User user) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.inflate(R.menu.chat_user_context_menu);
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_view_profile:
                    showUserProfile(user);
                    return true;
                    
                case R.id.action_delete_chat:
                    deleteConversation(user);
                    return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void showUserProfile(User user) {
        // Create a bottom sheet dialog to show user profile
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View bottomSheetView = LayoutInflater.from(context).inflate(
            R.layout.user_profile_bottom_sheet, null);
        
        // Initialize views
        ImageView profileImage = bottomSheetView.findViewById(R.id.profileImage);
        TextView username = bottomSheetView.findViewById(R.id.username);
        TextView email = bottomSheetView.findViewById(R.id.email);
        TextView language = bottomSheetView.findViewById(R.id.language);
        
        // Set user data
        username.setText(user.getUsername());
        email.setText(user.getEmail());
        language.setText("Language: " + user.getLanguage());
        
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none")) {
            Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.default_userpic)
                .into(profileImage);
        }
        
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void deleteConversation(User user) {
        new MaterialAlertDialogBuilder(context)
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this conversation? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                String roomId = generateRoomId(currentUserId, user.getUserId());
                FirebaseDatabase.getInstance().getReference("messages")
                    .child(roomId)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        CustomNotification.showNotification((Activity) context, 
                            "Conversation deleted", true);
                    })
                    .addOnFailureListener(e -> {
                        CustomNotification.showNotification((Activity) context, 
                            "Failed to delete conversation", false);
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private String generateRoomId(String userId1, String userId2) {
        String[] ids = {userId1, userId2};
        Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateList(List<User> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewUsername;
        private TextView textViewEmail;
        private ImageView imageViewUserPicture;
        public ImageView buttonMore;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            imageViewUserPicture = itemView.findViewById(R.id.imageViewUserPicture);
            buttonMore = itemView.findViewById(R.id.buttonMore);
        }
    }
}


