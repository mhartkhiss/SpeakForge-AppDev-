package com.example.appdev.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.ConversationModeActivity;
import com.example.appdev.R;
import com.example.appdev.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private String currentUserId; // Add current user ID field

    public UserAdapter(List<User> userList, Context context, String currentUserId) {
        this.userList = userList;
        this.context = context;
        this.currentUserId = currentUserId; // Initialize current user ID
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
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateList(List<User> newList) {
        userList = new ArrayList<>();
        userList.addAll(newList);
        notifyDataSetChanged();
    }

    public class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView textViewUsername;
        private TextView textViewEmail;
        private ImageView imageViewUserPicture; // Add ImageView for user picture
        private User currentUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            imageViewUserPicture = itemView.findViewById(R.id.imageViewUserPicture); // Initialize ImageView
            itemView.setOnClickListener(this);
        }

        public void bind(User user) {
            // Check if the current user is the same as the user being bound
            if (!user.getUserId().equals(currentUserId)) {
                currentUser = user;
                textViewUsername.setText(user.getUsername());
                textViewEmail.setText(user.getEmail());
                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none")) {
                    Glide.with(context).load(user.getProfileImageUrl()).into(imageViewUserPicture);
                } else {
                    imageViewUserPicture.setImageResource(R.drawable.default_userpic);
                }
            }
        }

        @Override
        public void onClick(View v) {
            // Handle click event here
            // Start ConversationModeActivity with the current user's information
            Intent intent = new Intent(context, ConversationModeActivity.class);
            intent.putExtra("userId", currentUser.getUserId()); // Pass the user's ID to the ConversationModeActivity
            intent.putExtra("username", currentUser.getUsername()); // Pass the username to the ConversationModeActivity
            intent.putExtra("email", currentUser.getEmail()); // Pass the email to the ConversationModeActivity
            intent.putExtra("profileImageUrl", currentUser.getProfileImageUrl()); // Pass the profile
            intent.putExtra("recipientLanguage", currentUser.getLanguage());
            context.startActivity(intent);
        }
    }
}


