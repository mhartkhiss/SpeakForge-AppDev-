package com.example.appdev.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.Variables;
import com.example.appdev.models.Message;
import com.example.appdev.R;
import com.example.appdev.subcontrollers.RegenerateMessageTranslation;
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

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messages;
    private DatabaseReference messagesRef;
    private String roomId;
    private List<TextView> visibleOriginalMessages;
    private Context context;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
        this.visibleOriginalMessages = new ArrayList<>();
    }

    public void setMessages(List<Message> messages) {
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

    public ChatAdapter(DatabaseReference messagesRef, String roomId, Context context) {
        this.messagesRef = messagesRef;
        this.roomId = roomId;
        this.context = context;
        messages = new ArrayList<>();
        this.visibleOriginalMessages = new ArrayList<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //ternary operator to determine which layout to inflate
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.item_message_sent : R.layout.item_message_received,
                parent, false);
        return new ChatViewHolder(view, messagesRef, roomId, visibleOriginalMessages, context);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Message message = messages.get(position);
        
        // Check if this message is part of consecutive messages from same sender
        boolean showAvatar = true;
        if (position < messages.size() - 1) {
            Message nextMessage = messages.get(position + 1);
            if (message.getSenderId().equals(nextMessage.getSenderId())) {
                showAvatar = false;
            }
        }
        
        holder.bind(message, showAvatar);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message == null || message.getSenderId() == null || 
            FirebaseAuth.getInstance().getCurrentUser() == null) {
            return 1; // Default to received message layout if any value is null
        }
        String senderId = message.getSenderId();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return senderId.equals(currentUserId) ? 0 : 1;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewMessage, textViewOriginalMessage;
        private DatabaseReference messagesRef;
        private String roomId;
        private List<TextView> visibleOriginalMessages;
        private de.hdodenhof.circleimageview.CircleImageView imageViewProfile;
        private DatabaseReference usersRef;
        private Context context;

        public ChatViewHolder(@NonNull View itemView, DatabaseReference messagesRef, String roomId, 
                List<TextView> visibleOriginalMessages, Context context) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            this.messagesRef = messagesRef;
            this.roomId = roomId;
            this.visibleOriginalMessages = visibleOriginalMessages;
            this.context = context;
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            usersRef = FirebaseDatabase.getInstance().getReference("users");
        }

        public void bind(Message message, boolean showAvatar) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
                loadProfileImage(currentUser.getUid());
                if (message.getMessageOG() != null) {
                    textViewMessage.setText(message.getMessageOG());
                } else {
                    textViewMessage.setText(message.getMessage());
                }

                textViewMessage.setOnClickListener(v -> {
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
                });
            } else {
                loadProfileImage(message.getSenderId());
                textViewMessage.setText(message.getMessage());

                // Change background color based on translation status
                androidx.cardview.widget.CardView cardView = 
                    (androidx.cardview.widget.CardView) textViewMessage.getParent();
                
                if (message.getMessage() != null && 
                    message.getMessageOG() != null && 
                    message.getMessage().equals(message.getMessageOG())) {
                    // Message is not translated - use light gray
                    cardView.setCardBackgroundColor(itemView.getContext()
                        .getResources().getColor(R.color.light_gray));
                } else {
                    // Message is translated - use original orange color
                    cardView.setCardBackgroundColor(itemView.getContext()
                        .getResources().getColor(R.color.message_received_bg));
                }

                // Show/hide avatar based on consecutive messages
                if (imageViewProfile != null) {
                    imageViewProfile.setVisibility(showAvatar ? View.VISIBLE : View.INVISIBLE);
                }

                // View original message on single click
                textViewMessage.setOnClickListener(v -> handleOriginalMessageClick(message));

                // Replace the long click listener with the new context menu
                textViewMessage.setOnLongClickListener(v -> {
                    if (Variables.userAccountType.equals("free")) {
                        Toast.makeText(itemView.getContext(), 
                            "Premium features are not available in free version", 
                            Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    showContextMenu(v, message);
                    return true;
                });
            }
        }

        private void handleOriginalMessageClick(Message message) {
            // Don't do anything if message is same as original
            if (message.getMessage() != null && 
                message.getMessageOG() != null && 
                message.getMessage().equals(message.getMessageOG())) {
                return;
            }

            // Check if the user is a free user
            if(Variables.userAccountType.equals("free")){
                textViewOriginalMessage.setVisibility(View.VISIBLE);
                textViewOriginalMessage.setText("You can view the original message by upgrading to a premium account.");
                textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.purple_200));
                return;
            }
            
            // If original message is already visible, hide it
            if (textViewOriginalMessage.getVisibility() == View.VISIBLE) {
                textViewOriginalMessage.setVisibility(View.GONE);
                visibleOriginalMessages.remove(textViewOriginalMessage);
                return;
            }
            
            // Hide all other visible original messages
            for (TextView textView : visibleOriginalMessages) {
                textView.setVisibility(View.GONE);
            }
            visibleOriginalMessages.clear();

            // Show this message's original text
            textViewOriginalMessage.setVisibility(View.VISIBLE);
            textViewOriginalMessage.setText(message.getMessageOG());
            textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
            visibleOriginalMessages.add(textViewOriginalMessage);
        }

        private void handleMessageTranslationClick(Message message) {
            // Check if the user is a free user
            if(Variables.userAccountType.equals("free")){
                Toast.makeText(itemView.getContext(), "You need to upgrade to regenerate translations", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the current message text
            String currentMessage = textViewMessage.getText().toString();
            String messageId = message.getMessageId();

            for (TextView textView : visibleOriginalMessages) {
                textView.setVisibility(View.GONE);
            }
            visibleOriginalMessages.clear();

            List<String> messageVariations = new ArrayList<>();
            if (message.getMessageVar1() == null && !textViewMessage.getText().toString().equals("......")) {
                String textMessage = message.getMessageOG();
                String textLanguage = Variables.userLanguage;
                textViewMessage.setText("......");
                RegenerateMessageTranslation regenerateMessageTranslation = new RegenerateMessageTranslation(context);
                regenerateMessageTranslation.setOnTranslationRegeneratedListener(newTranslation -> {
                    textViewMessage.setText(newTranslation);
                });
                regenerateMessageTranslation.regenerate(textMessage, message.getMessageId(), textLanguage);
                return;
            }

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

        private void showContextMenu(View anchor, Message message) {
            // Create and show the popup window
            View popupView = LayoutInflater.from(anchor.getContext())
                    .inflate(R.layout.message_context_menu, null);
            
            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            // Add elevation for shadow effect
            popupWindow.setElevation(10);

            // Set up click listeners for menu items
            TextView regenerateItem = popupView.findViewById(R.id.menuItemRegenerate);
            TextView toggleOriginalItem = popupView.findViewById(R.id.menuItemToggleOriginal);
            TextView removeTranslationItem = popupView.findViewById(R.id.menuItemRemoveTranslation);

            // Set the appropriate text for regenerate/translate button
            boolean isUntranslated = message.getMessage() != null && 
                message.getMessageOG() != null && 
                message.getMessage().equals(message.getMessageOG());

            if (isUntranslated) {
                regenerateItem.setText("Translate");
                // Hide the toggle original message and remove translation options for untranslated messages
                toggleOriginalItem.setVisibility(View.GONE);
                removeTranslationItem.setVisibility(View.GONE);
            } else {
                regenerateItem.setText("Regenerate Translation");
                // Update toggle text based on current state
                if (textViewOriginalMessage.getVisibility() == View.VISIBLE) {
                    toggleOriginalItem.setText("Hide Original Message");
                } else {
                    toggleOriginalItem.setText("Show Original Message");
                }
                removeTranslationItem.setVisibility(View.VISIBLE);
            }

            regenerateItem.setOnClickListener(v -> {
                handleMessageTranslationClick(message);
                popupWindow.dismiss();
            });

            toggleOriginalItem.setOnClickListener(v -> {
                handleOriginalMessageClick(message);
                popupWindow.dismiss();
            });

            removeTranslationItem.setOnClickListener(v -> {
                if (roomId != null && message.getMessageId() != null) {
                    // Update the message in Firebase to the original message
                    messagesRef.child(roomId)
                            .child(message.getMessageId())
                            .child("message")
                            .setValue(message.getMessageOG());
                }
                popupWindow.dismiss();
            });

            // Show the popup window below the anchor view
            popupWindow.showAsDropDown(anchor, 0, -anchor.getHeight());
        }
    }
}
