package com.example.appdev.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.R;
import com.example.appdev.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder> {

    private final List<User> userList;
    private final Context context;
    private final String currentUserId;
    private final OnUserClickListener onUserClickListener;

    public interface OnUserClickListener {
        void onUserClicked(User user);
    }

    public SearchUserAdapter(List<User> userList, Context context, String currentUserId, OnUserClickListener listener) {
        this.userList = userList;
        this.context = context;
        this.currentUserId = currentUserId;
        this.onUserClickListener = listener;
    }

    @NonNull
    @Override
    public SearchUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new SearchUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchUserViewHolder holder, int position) {
        User user = userList.get(position);

        // Set username
        holder.textViewUsername.setText(user.getUsername());

        // Show language and email info
        String subtitle = "Language: " + user.getLanguage();
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            subtitle += " • " + user.getEmail();
        }
        holder.textViewEmail.setText(subtitle);
        holder.textViewEmail.setTextColor(holder.itemView.getContext()
                .getResources().getColor(R.color.grey));
        holder.textViewEmail.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none")) {
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.default_userpic)
                    .into(holder.imageViewUserPicture);
        } else {
            holder.imageViewUserPicture.setImageResource(R.drawable.default_userpic);
        }

        // Hide the more button for search results
        holder.buttonMore.setVisibility(View.GONE);

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onUserClicked(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class SearchUserViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewUsername;
        private TextView textViewEmail;
        private CircleImageView imageViewUserPicture;
        public ImageView buttonMore;

        public SearchUserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewUsername = itemView.findViewById(R.id.textViewUsername);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            imageViewUserPicture = itemView.findViewById(R.id.imageViewUserPicture);
            buttonMore = itemView.findViewById(R.id.buttonMore);
        }
    }
}
