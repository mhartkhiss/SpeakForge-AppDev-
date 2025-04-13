package com.example.appdev.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.Variables;
import com.example.appdev.models.Message;
import com.example.appdev.R;
import com.example.appdev.subcontrollers.RegenerateMessageTranslation;
import com.example.appdev.utils.LoadingDotsView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        private LoadingDotsView loadingDots;
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
            loadingDots = itemView.findViewById(R.id.loadingDots);
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
                // This is a sent message (from current user)
                loadProfileImage(currentUser.getUid());
                
                // For sent messages, always show the original message
                textViewMessage.setText(message.getMessage());
                
                // Handle possible viewing of translations
                textViewMessage.setOnClickListener(v -> {
                    // Toggle between original message and translated message if different
                    Map<String, String> translations = message.getTranslations();
                    
                    if (translations != null && translations.containsKey("translation1") && 
                        !message.getMessage().equals(translations.get("translation1"))) {
                        
                        if (textViewMessage.getText().toString().equals(message.getMessage())) {
                            textViewMessage.setTypeface(null, Typeface.NORMAL);
                            textViewMessage.setText(translations.get("translation1"));
                        } else {
                            textViewMessage.setTypeface(null, Typeface.ITALIC);
                            textViewMessage.setText(message.getMessage());
                        }
                    }
                });
            } else {
                // This is a received message (from other user)
                loadProfileImage(message.getSenderId());
                
                // Handle loading state for received messages
                Map<String, String> translations = message.getTranslations();
                String translationState = message.getTranslationState();
                
                // Check if message is in loading state (being translated)
                boolean isLoading = "TRANSLATING".equals(translationState);
                
                if (isLoading) {
                    textViewMessage.setVisibility(View.GONE);
                    loadingDots.setVisibility(View.VISIBLE);
                    loadingDots.startAnimation();
                } else {
                    textViewMessage.setVisibility(View.VISIBLE);
                    loadingDots.setVisibility(View.GONE);
                    loadingDots.stopAnimation();
                    
                    // For received messages, show the translation if available and not removed
                    if (translations != null && translations.containsKey("translation1") && 
                        !"REMOVED".equals(translationState)) {
                        // Display translation1 by default
                        textViewMessage.setText(translations.get("translation1"));
                    } else {
                        // Show original message if no translation is available or translations were removed
                        textViewMessage.setText(message.getMessage());
                    }
                }

                // Always use the same background color regardless of translation status
                View cardView = (View) textViewMessage.getParent().getParent();
                if (cardView instanceof CardView) {
                    CardView messageCard = (CardView) cardView;
                    messageCard.setCardBackgroundColor(itemView.getContext()
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
            // For free users, show premium upgrade message
            if(Variables.userAccountType.equals("free")){
                textViewOriginalMessage.setVisibility(View.VISIBLE);
                textViewOriginalMessage.setText("You can view the original message by upgrading to a premium account.");
                textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.purple_200));
                // Don't add to visibleOriginalMessages list for the premium message
                return;
            }
            
            // Check if the current original message view is visible
            boolean wasVisible = textViewOriginalMessage.getVisibility() == View.VISIBLE;

            // Hide any other currently visible original messages
            // Use a copy to avoid ConcurrentModificationException if list is modified elsewhere
            List<TextView> currentlyVisible = new ArrayList<>(visibleOriginalMessages); 
            for (TextView visibleTextView : currentlyVisible) {
                if (visibleTextView != textViewOriginalMessage) { // Don't hide self yet
                   visibleTextView.setVisibility(View.GONE);
                }
            }
            visibleOriginalMessages.clear(); // Clear the main list

            // For premium users, toggle the original message display
            if (message.getMessage() != null) {
                if (wasVisible) {
                    // If it was visible, hide it
                    textViewOriginalMessage.setVisibility(View.GONE);
                    // No need to add to visibleOriginalMessages as it's now hidden
                } else {
                    // If it was hidden, show it and add to the list
                    textViewOriginalMessage.setVisibility(View.VISIBLE);
                    textViewOriginalMessage.setText(message.getMessage());
                    textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
                    visibleOriginalMessages.add(textViewOriginalMessage); // Track this as visible
                }
            }
        }

        private void handleMessageTranslationClick(Message message) {
            Map<String, String> translations = message.getTranslations();
            String currentTranslation = textViewMessage.getText().toString();
            
            // Check if we have multiple translations
            if (translations != null) {
                String translation1 = translations.get("translation1");
                String translation2 = translations.get("translation2");
                String translation3 = translations.get("translation3");
                
                // Only cycle through translations that actually exist and aren't null or empty
                if (translation1 != null && !translation1.isEmpty() && 
                    translation2 != null && !translation2.isEmpty() && 
                    translation3 != null && !translation3.isEmpty()) {
                    
                    // If we have all three translations, cycle through them
                    if (currentTranslation.equals(translation1)) {
                        textViewMessage.setText(translation2);
                    } else if (currentTranslation.equals(translation2)) {
                        textViewMessage.setText(translation3);
                    } else {
                        textViewMessage.setText(translation1);
                    }
                    return;
                }
            }
            
            // If we don't have multiple translations or couldn't cycle through them,
            // generate new translations
            String originalMessage = message.getMessage();
            if (originalMessage == null || originalMessage.isEmpty()) {
                return;
            }
            
            // Get target language (current user's language)
            String targetLanguage = Variables.userLanguage;
            
            // Create regeneration handler
            RegenerateMessageTranslation regenerator = new RegenerateMessageTranslation(context);
            
            // Trigger regeneration
            regenerator.regenerate(originalMessage, message.getMessageId(), targetLanguage);
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

            // Determine if message has translations or is untranslated
            boolean hasTranslations = message.getTranslations() != null && 
                                     message.getTranslations().containsKey("translation1");

            if (!hasTranslations) {
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
                if (message.getMessageId() != null) {
                    // Get the original message
                    String originalMessage = message.getMessage();
                    
                    if (originalMessage != null) {
                        DatabaseReference messageRef = messagesRef.child(Variables.roomId)
                                .child(message.getMessageId());
                        
                        // Update translation state to REMOVED
                        messageRef.child("translationState").setValue("REMOVED");
                        // Remove all translations
                        messageRef.child("translations").removeValue();
                    }
                }
                popupWindow.dismiss();
            });

            // Show the popup window below the anchor view
            popupWindow.showAsDropDown(anchor, 0, -anchor.getHeight());
        }
    }
}
