package com.example.appdev.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appdev.models.VoiceMessage;
import com.example.appdev.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VoiceMessageAdapter extends RecyclerView.Adapter<VoiceMessageAdapter.VoiceMessageViewHolder> {

    private List<VoiceMessage> messages;
    private DatabaseReference messagesRef;
    private String roomId;

    public VoiceMessageAdapter(DatabaseReference messagesRef, String roomId) {
        this.messagesRef = messagesRef;
        this.roomId = roomId;
    }

    @NonNull
    @Override
    public VoiceMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_voice_message, parent, false);
        return new VoiceMessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoiceMessageViewHolder holder, int position) {
        VoiceMessage message = messages.get(position);
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isCurrentUser = message.getSenderId().equals(currentUserId);

        // Set message alignment (left for received, right for sent)
        if (isCurrentUser) {
            holder.leftMessageContainer.setVisibility(View.GONE);
            holder.rightMessageContainer.setVisibility(View.VISIBLE);

            // Show original voice text on the right
            holder.rightVoiceText.setText(message.getVoiceText());

            // Show translated text if available
            if (message.getTranslatedText() != null && !message.getTranslatedText().isEmpty()) {
                holder.rightTranslatedText.setText(message.getTranslatedText());
                holder.rightTranslatedText.setVisibility(View.VISIBLE);
            } else {
                holder.rightTranslatedText.setVisibility(View.GONE);
            }

            // Show timestamp
            holder.rightTimestamp.setText(formatTimestamp(message.getTimestamp()));

        } else {
            holder.rightMessageContainer.setVisibility(View.GONE);
            holder.leftMessageContainer.setVisibility(View.VISIBLE);

            // Show original voice text on the left
            holder.leftVoiceText.setText(message.getVoiceText());

            // Show translated text if available
            if (message.getTranslatedText() != null && !message.getTranslatedText().isEmpty()) {
                holder.leftTranslatedText.setText(message.getTranslatedText());
                holder.leftTranslatedText.setVisibility(View.VISIBLE);
            } else {
                holder.leftTranslatedText.setVisibility(View.GONE);
            }

            // Show timestamp
            holder.leftTimestamp.setText(formatTimestamp(message.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public void setMessages(List<VoiceMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class VoiceMessageViewHolder extends RecyclerView.ViewHolder {
        // Left message (received)
        View leftMessageContainer;
        TextView leftVoiceText;
        TextView leftTranslatedText;
        TextView leftTimestamp;

        // Right message (sent)
        View rightMessageContainer;
        TextView rightVoiceText;
        TextView rightTranslatedText;
        TextView rightTimestamp;

        VoiceMessageViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize left message views
            leftMessageContainer = itemView.findViewById(R.id.leftMessageContainer);
            leftVoiceText = itemView.findViewById(R.id.leftVoiceText);
            leftTranslatedText = itemView.findViewById(R.id.leftTranslatedText);
            leftTimestamp = itemView.findViewById(R.id.leftTimestamp);

            // Initialize right message views
            rightMessageContainer = itemView.findViewById(R.id.rightMessageContainer);
            rightVoiceText = itemView.findViewById(R.id.rightVoiceText);
            rightTranslatedText = itemView.findViewById(R.id.rightTranslatedText);
            rightTimestamp = itemView.findViewById(R.id.rightTimestamp);
        }
    }
}
