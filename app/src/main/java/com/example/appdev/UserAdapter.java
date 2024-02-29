package com.example.appdev;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;

    public UserAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
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

    public class UserViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView textViewUsername;
        private TextView textViewEmail;
        private User currentUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            itemView.setOnClickListener(this);
        }

        public void bind(User user) {
            currentUser = user;
            textViewUsername.setText(user.getUsername());
            textViewEmail.setText(user.getEmail());
        }

        @Override
        public void onClick(View v) {
            // Handle click event here
            // Start ConversationModeActivity with the current user's information
            Intent intent = new Intent(context, ConversationModeActivity.class);
            intent.putExtra("userId", currentUser.getUserId()); // Pass the user's ID to the ConversationModeActivity
            intent.putExtra("username", currentUser.getUsername()); // Pass the username to the ConversationModeActivity
            intent.putExtra("email", currentUser.getEmail()); // Pass the email to the ConversationModeActivity
            context.startActivity(intent);
        }
    }
}

