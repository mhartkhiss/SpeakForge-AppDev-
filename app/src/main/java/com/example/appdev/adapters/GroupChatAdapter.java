package com.example.appdev.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.R;
import com.example.appdev.Variables;
import com.example.appdev.models.GroupMessage;
import com.example.appdev.subcontrollers.RegenerateMessageTranslation;
import com.example.appdev.utils.LoadingDotsView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder> {

    private List<GroupMessage> messages;
    private DatabaseReference messagesRef;
    private String groupId;
    private Context context;
    private Map<String, String> usernameCache = new HashMap<>();
    private DatabaseReference usersRef;
    private List<TextView> visibleOriginalMessages;

    public GroupChatAdapter() {
        this.messages = new ArrayList<>();
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        this.visibleOriginalMessages = new ArrayList<>();
        setupUsernameCacheListener();
    }

    public GroupChatAdapter(DatabaseReference messagesRef, String groupId, Context context) {
        this.messagesRef = messagesRef;
        this.groupId = groupId;
        this.context = context;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        messages = new ArrayList<>();
        this.visibleOriginalMessages = new ArrayList<>();
        setupUsernameCacheListener();
    }

    private void setupUsernameCacheListener() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    if (username != null && !username.isEmpty()) {
                        usernameCache.put(userId, username);
                    }
                }
                // Refresh the view to update usernames
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GroupChatAdapter", "Error loading usernames: " + error.getMessage());
            }
        });
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
        return new GroupChatViewHolder(view, context, groupId, visibleOriginalMessages);
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
        
        holder.bind(message, showSenderInfo, usernameCache);
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
        private TextView textViewMessage, textViewOriginalMessage;
        private TextView textViewSenderName;
        private LoadingDotsView loadingDots;
        private de.hdodenhof.circleimageview.CircleImageView imageViewProfile;
        private Context context;
        private CardView messageCard;
        private DatabaseReference messagesRef;
        private List<TextView> visibleOriginalMessages;
        private String groupId;
        private Map<String, String> usernameCache;

        public GroupChatViewHolder(@NonNull View itemView, Context context, String groupId, List<TextView> visibleOriginalMessages) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            loadingDots = itemView.findViewById(R.id.loadingDots);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            this.context = context;
            this.groupId = groupId;
            this.visibleOriginalMessages = visibleOriginalMessages;
            messageCard = itemView.findViewById(R.id.cardMessage);
            messagesRef = FirebaseDatabase.getInstance().getReference("group_messages");

            // Handle long click on the message
            if (textViewMessage != null) {
                textViewMessage.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // Using tag to store the message object
                        GroupMessage message = (GroupMessage) textViewMessage.getTag();
                        if (message != null) {
                            showPopupMenu(v, message);
                            return true;
                        }
                    }
                    return false;
                });
            }
        }

        public void bind(GroupMessage message, boolean showSenderInfo, Map<String, String> usernameCache) {
            this.usernameCache = usernameCache;
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            // Store the message object as a tag for reference in click handlers
            textViewMessage.setTag(message);
            
            // Define variables at the beginning of the method to make them accessible throughout
            String userLanguage = Variables.userLanguage;
            String originalLanguage = message.getSenderLanguage();
            Map<String, String> translations = message.getTranslations();

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

            // Load and show sender name for received messages using senderId
            if (textViewSenderName != null) {
                textViewSenderName.setVisibility(showSenderInfo ? View.VISIBLE : View.GONE);
                if (showSenderInfo) {
                    String senderId = message.getSenderId();
                    if (senderId != null && !senderId.isEmpty()) {
                        // Use cached username
                        String username = usernameCache.get(senderId);
                        if (username != null && !username.isEmpty()) {
                            textViewSenderName.setText(username);
                        } else {
                            textViewSenderName.setText("User");
                        }
                    } else {
                        textViewSenderName.setText("User");
                    }
                }
            }

            if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
                // Sent messages
                String originalText = null;
                
                if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                    originalText = translations.get(originalLanguage);
                    textViewMessage.setText(originalText);
                    
                    // Remove click listener - we don't want to toggle between translations for sent messages
                    textViewMessage.setOnClickListener(null);
                } else {
                    textViewMessage.setText(message.getMessage());
                }
            } else {
                // Received messages
                // Handle loading state for messages being translated
                // Show loading state if:
                // 1. Message is recent (within last 5 seconds) AND translations is empty, OR
                // 2. Translations map is null
                long currentTime = System.currentTimeMillis();
                boolean isRecentMessage = (currentTime - message.getTimestamp()) < 5000; // 5 seconds
                boolean isTranslating = (translations == null) || (translations.isEmpty() && isRecentMessage);

                if (isTranslating) {
                    // Show loading state while translations are being generated
                    textViewMessage.setVisibility(View.GONE);
                    if (loadingDots != null) {
                        loadingDots.setVisibility(View.VISIBLE);
                        loadingDots.startAnimation();
                    }
                } else {
                    // Translations exist or message is old enough to show original
                    textViewMessage.setVisibility(View.VISIBLE);
                    if (loadingDots != null) {
                        loadingDots.setVisibility(View.GONE);
                        loadingDots.stopAnimation();
                    }

                    // If we have a translation in user's language, show it
                    if (translations.containsKey(userLanguage)) {
                        textViewMessage.setText(translations.get(userLanguage));
                    } else {
                        // No translation in user's language, show original message
                        textViewMessage.setText(message.getMessage());
                    }
                }

                // Set click listener for received messages to show original
                textViewMessage.setOnClickListener(v -> handleOriginalMessageClick(message));
            }
        }

        private void handleOriginalMessageClick(GroupMessage message) {
            // Check if the current original message view is visible
            boolean wasVisible = textViewOriginalMessage.getVisibility() == View.VISIBLE;
            
            // Hide any other currently visible original messages
            // Use a copy to avoid ConcurrentModificationException
            List<TextView> currentlyVisible = new ArrayList<>(visibleOriginalMessages);
            for (TextView visibleTextView : currentlyVisible) {
                if (visibleTextView != textViewOriginalMessage) { // Don't hide self yet
                    visibleTextView.setVisibility(View.GONE);
                }
            }
            visibleOriginalMessages.clear(); // Clear the main list

            // Show this message's original text if it wasn't visible, or hide if it was
            if (textViewOriginalMessage != null) {
                String originalLanguage = message.getSenderLanguage();
                Map<String, String> translations = message.getTranslations();
                
                String originalText = null;
                if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                    originalText = translations.get(originalLanguage);
                } else {
                    // Fallback to message field
                    originalText = message.getMessage();
                }
                
                if (originalText != null) {
                    if (wasVisible) {
                        // If it was visible, hide it
                        textViewOriginalMessage.setVisibility(View.GONE);
                        // No need to add to list
                    } else {
                        // If it was hidden, show it and add to the list
                        textViewOriginalMessage.setVisibility(View.VISIBLE);
                        textViewOriginalMessage.setText(originalText);
                        textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
                        visibleOriginalMessages.add(textViewOriginalMessage); // Track this as visible
                    }
                }
            }
        }

        private void showPopupMenu(View view, GroupMessage message) {
            PopupMenu popupMenu = new PopupMenu(context, view);
            popupMenu.inflate(R.menu.chat_message_menu);

            // Get menu items
            String userLanguage = Variables.userLanguage;
            String senderLanguage = message.getSenderLanguage();
            Map<String, String> translations = message.getTranslations();
            
            boolean isUntranslated = senderLanguage != null && 
                                   userLanguage != null && 
                                   senderLanguage.equals(userLanguage);

            // Only show remove translation for your own messages
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            boolean isUserMessage = currentUser != null && 
                                  message.getSenderId().equals(currentUser.getUid());

            if (!isUserMessage) {
                popupMenu.getMenu().findItem(R.id.action_remove_translation).setVisible(false);
            }

            // Update menu item text
            if (isUntranslated) {
                popupMenu.getMenu().findItem(R.id.action_regenerate).setTitle("Translate");
                popupMenu.getMenu().findItem(R.id.action_toggle_original).setVisible(false);
                popupMenu.getMenu().findItem(R.id.action_remove_translation).setVisible(false);
            } else {
                popupMenu.getMenu().findItem(R.id.action_regenerate).setTitle("Regenerate Translation");
                popupMenu.getMenu().findItem(R.id.action_toggle_original).setVisible(true);
                
                // Update toggle text based on current state
                if (textViewOriginalMessage != null && textViewOriginalMessage.getVisibility() == View.VISIBLE) {
                    popupMenu.getMenu().findItem(R.id.action_toggle_original).setTitle("Hide Original Message");
                } else {
                    popupMenu.getMenu().findItem(R.id.action_toggle_original).setTitle("Show Original Message");
                }
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                
                if (itemId == R.id.action_regenerate) {
                    handleMessageTranslationClick(message);
                    return true;
                } else if (itemId == R.id.action_toggle_original) {
                    handleOriginalMessageClick(message);
                    return true;
                } else if (itemId == R.id.action_remove_translation) {
                    if (message.getMessageId() != null) {
                        // Update the message in Firebase to use original language translation
                        // and remove other language translations
                        String originalLanguage = message.getSenderLanguage();
                        String originalMessage = null;
                        
                        if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                            originalMessage = translations.get(originalLanguage);
                        } else {
                            // Fallback to message field
                            originalMessage = message.getMessage();
                        }
                        
                        if (originalMessage != null) {
                            DatabaseReference messageRef = messagesRef.child(groupId)
                                    .child(message.getMessageId());
                            messageRef.child("message").setValue(originalMessage);
                            
                            // Remove all translations except the original language
                            for (String lang : translations.keySet()) {
                                if (!lang.equals(originalLanguage)) {
                                    messageRef.child("translations").child(lang).removeValue();
                                }
                            }
                        }
                    }
                    return true;
                }
                
                return false;
            });

            popupMenu.show();
        }

        private void handleMessageTranslationClick(GroupMessage message) {
            // Check if the user is a free user
            if(Variables.userAccountType.equals("free")){
                Toast.makeText(itemView.getContext(), 
                    "You need to upgrade to regenerate translations", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            String messageId = message.getMessageId();

            // Hide any visible original messages
            for (TextView textView : visibleOriginalMessages) {
                textView.setVisibility(View.GONE);
            }
            visibleOriginalMessages.clear();

            // Get the original message from translations
            String originalLanguage = message.getSenderLanguage();
            Map<String, String> translations = message.getTranslations();
            String originalMessage = null;
            
            if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                originalMessage = translations.get(originalLanguage);
            } else {
                originalMessage = message.getMessage(); // Fallback to current message
            }

            if (originalMessage == null) {
                Toast.makeText(itemView.getContext(), 
                    "Cannot translate without original message", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a final copy of the original message for use in lambdas
            final String finalOriginalMessage = originalMessage;

            // Show loading indicator
            textViewMessage.setVisibility(View.GONE);
            if (loadingDots != null) {
                loadingDots.setVisibility(View.VISIBLE);
                loadingDots.startAnimation();
            }

            // Get source language from the message
            String sourceLanguage = message.getSenderLanguage();
            if (sourceLanguage == null) {
                sourceLanguage = "auto"; // Default to auto if not found
            }

            // Get the current user's language
            String targetLanguage = Variables.userLanguage;

            // If the source and target languages match, no need to translate
            if (sourceLanguage.equals(targetLanguage)) {
                // Just update the translations map, not the message field
                messagesRef.child(groupId).child(messageId)
                    .child("translations")
                    .child(targetLanguage)
                    .setValue(finalOriginalMessage)
                    .addOnSuccessListener(aVoid -> {
                        // Hide loading indicator and show message again
                        if (loadingDots != null) {
                            loadingDots.setVisibility(View.GONE);
                            loadingDots.stopAnimation();
                        }
                        textViewMessage.setVisibility(View.VISIBLE);
                        textViewMessage.setText(finalOriginalMessage);
                    });
                return;
            }

            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("text", finalOriginalMessage);
                requestBody.put("source_language", sourceLanguage);
                requestBody.put("target_language", targetLanguage);
                requestBody.put("variants", "single");
                requestBody.put("model", Variables.userTranslator.toLowerCase());
                requestBody.put("room_id", groupId);
                requestBody.put("message_id", messageId);
                requestBody.put("is_group", true);

                // Get translation mode from Firebase
                messagesRef.child(groupId).child(messageId).child("translationMode")
                    .get().addOnCompleteListener(modeTask -> {
                        String translationMode;
                        if (modeTask.isSuccessful() && modeTask.getResult() != null 
                                && modeTask.getResult().getValue() != null) {
                            translationMode = modeTask.getResult().getValue(String.class);
                        } else {
                            translationMode = Variables.isFormalTranslationMode ? "formal" : "casual";
                        }
                        
                        try {
                            requestBody.put("translation_mode", translationMode);
                            
                            // Make the API request
                            new AsyncTask<JSONObject, Void, Boolean>() {
                                @Override
                                protected Boolean doInBackground(JSONObject... params) {
                                    try {
                                        JSONObject requestBody = params[0];
                                        URL url = new URL(Variables.API_REGENERATE_TRANSLATION_URL);
                                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                        conn.setRequestMethod("POST");
                                        conn.setRequestProperty("Content-Type", "application/json");
                                        conn.setDoOutput(true);

                                        try (OutputStream os = conn.getOutputStream()) {
                                            byte[] input = requestBody.toString().getBytes("utf-8");
                                            os.write(input, 0, input.length);
                                        }

                                        int responseCode = conn.getResponseCode();
                                        
                                        if (responseCode == HttpURLConnection.HTTP_OK) {
                                            try (BufferedReader br = new BufferedReader(
                                                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                                                String line;
                                                StringBuilder response = new StringBuilder();
                                                while ((line = br.readLine()) != null) {
                                                    response.append(line);
                                                }
                                                Log.d("GroupChatAdapter", "Translation response: " + response.toString());
                                                return true;
                                            }
                                        } else {
                                            Log.e("GroupChatAdapter", "Error response code: " + responseCode);
                                            return false;
                                        }
                                    } catch (Exception e) {
                                        Log.e("GroupChatAdapter", "Translation error: " + e.getMessage());
                                        return false;
                                    }
                                }

                                @Override
                                protected void onPostExecute(Boolean success) {
                                    super.onPostExecute(success);
                                    // Hide loading indicator and show message text again
                                    if (loadingDots != null) {
                                        loadingDots.setVisibility(View.GONE);
                                        loadingDots.stopAnimation();
                                    }
                                    textViewMessage.setVisibility(View.VISIBLE);
                                    
                                    if (!success) {
                                        Toast.makeText(itemView.getContext(), 
                                            "Translation regeneration failed", Toast.LENGTH_SHORT).show();
                                        // Optional: Revert text to original/previous state if needed,
                                        // but ideally bind() handles this based on data.
                                    }
                                }
                            }.execute(requestBody);

                        } catch (Exception e) {
                            Log.e("GroupChatAdapter", "Error preparing translation: " + e.getMessage());
                            // Hide loading indicator
                            if (loadingDots != null) {
                                loadingDots.setVisibility(View.GONE);
                                loadingDots.stopAnimation();
                            }
                            textViewMessage.setVisibility(View.VISIBLE);
                        }
                    });

            } catch (Exception e) {
                Log.e("GroupChatAdapter", "Error preparing translation: " + e.getMessage());
                // Hide loading indicator
                if (loadingDots != null) {
                    loadingDots.setVisibility(View.GONE);
                    loadingDots.stopAnimation();
                }
                textViewMessage.setVisibility(View.VISIBLE);
            }
        }
    }
}
