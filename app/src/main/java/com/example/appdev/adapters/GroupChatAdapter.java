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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder> {

    private List<GroupMessage> messages;
    private DatabaseReference messagesRef;
    private String groupId;
    private Context context;

    public GroupChatAdapter() {
        this.messages = new ArrayList<>();
    }

    public GroupChatAdapter(DatabaseReference messagesRef, String groupId, Context context) {
        this.messagesRef = messagesRef;
        this.groupId = groupId;
        this.context = context;
        messages = new ArrayList<>();
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
        return new GroupChatViewHolder(view, context, groupId);
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
        
        holder.bind(message, showSenderInfo);
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
        private List<TextView> visibleOriginalMessages = new ArrayList<>();
        private String groupId;

        public GroupChatViewHolder(@NonNull View itemView, Context context, String groupId) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            loadingDots = itemView.findViewById(R.id.loadingDots);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            this.context = context;
            this.groupId = groupId;
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

        public void bind(GroupMessage message, boolean showSenderInfo) {
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
                    // Use a loading placeholder until we load the real name
                    textViewSenderName.setText("Loading...");
                    
                    // Get the sender's name from Firebase users database
                    String senderId = message.getSenderId();
                    if (senderId != null && !senderId.isEmpty()) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(senderId);
                        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String username = snapshot.getValue(String.class);
                                    if (username != null && !username.isEmpty()) {
                                        textViewSenderName.setText(username);
                                    } else {
                                        textViewSenderName.setText("User");
                                    }
                                } else {
                                    textViewSenderName.setText("User");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                textViewSenderName.setText("User");
                            }
                        });
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
                // Check if translations exists yet - if not, show loading indicator
                if (translations == null || translations.isEmpty()) {
                    textViewMessage.setVisibility(View.GONE);
                    if (loadingDots != null) {
                        loadingDots.setVisibility(View.VISIBLE);
                        loadingDots.startAnimation();
                    }
                } else {
                    textViewMessage.setVisibility(View.VISIBLE);
                    if (loadingDots != null) {
                        loadingDots.setVisibility(View.GONE);
                        loadingDots.stopAnimation();
                    }
                    
                    // Check if this message has translations and if there's one for this user's language
                    if (translations.containsKey(userLanguage)) {
                        // Show message in user's language
                        String translatedText = translations.get(userLanguage);
                        textViewMessage.setText(translatedText);
                    } else {
                        // No translation available, show the message as is
                        textViewMessage.setText(message.getMessage());
                    }

                    // Set click listener for received messages to show original
                    textViewMessage.setOnClickListener(v -> {
                        String originalText = null;
                        
                        if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                            originalText = translations.get(originalLanguage);
                        } else {
                            // Fallback to message field if sender's language is not in translations
                            originalText = message.getMessage();
                        }
                        
                        if (originalText != null && !textViewMessage.getText().toString().equals(originalText)) {
                            // Toggle between translation and original
                            if (textViewOriginalMessage != null) {
                                if (textViewOriginalMessage.getVisibility() == View.VISIBLE) {
                                    textViewOriginalMessage.setVisibility(View.GONE);
                                } else {
                                    textViewOriginalMessage.setVisibility(View.VISIBLE);
                                    textViewOriginalMessage.setText(originalText);
                                }
                            }
                        }
                    });
                }

                // Set original message text visibility (hidden by default)
                if (textViewOriginalMessage != null) {
                    // Prepare the original text but keep it hidden
                    String originalText = null;
                    if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                        originalText = translations.get(originalLanguage);
                    } else {
                        // Fallback to message field
                        originalText = message.getMessage();
                    }
                    
                    if (originalText != null) {
                        // Set the text but keep it hidden
                        textViewOriginalMessage.setVisibility(View.GONE);
                        textViewOriginalMessage.setText(originalText);
                        textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
                        // Don't add to visible messages list since it's not visible
                    }
                }
            }
        }

        private void handleOriginalMessageClick(GroupMessage message) {
            // Hide any visible original messages
            for (TextView textView : visibleOriginalMessages) {
                textView.setVisibility(View.GONE);
            }
            visibleOriginalMessages.clear();

            // Show this message's original text
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
                    textViewOriginalMessage.setVisibility(View.VISIBLE);
                    textViewOriginalMessage.setText(originalText);
                    textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
                    visibleOriginalMessages.add(textViewOriginalMessage);
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
                                    
                                    if (success) {
                                        // Get the new translation from Firebase after regeneration
                                        messagesRef.child(groupId).child(messageId)
                                            .child("translations").child(targetLanguage)
                                            .get().addOnSuccessListener(dataSnapshot -> {
                                                if (dataSnapshot.exists()) {
                                                    String newTranslation = dataSnapshot.getValue(String.class);
                                                    if (newTranslation != null) {
                                                        // Update UI with the new translation
                                                        textViewMessage.setText(newTranslation);
                                                    }
                                                }
                                            });
                                    } else {
                                        Toast.makeText(itemView.getContext(), 
                                            "Translation failed", Toast.LENGTH_SHORT).show();
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
