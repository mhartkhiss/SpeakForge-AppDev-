package com.example.appdev.adapter;

import android.graphics.Typeface;
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

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the appropriate layout based on the senderId
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.item_message_sent : R.layout.item_message_received,
                parent, false);
        return new ChatViewHolder(view);
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

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);

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
            }
        }


    }
}




