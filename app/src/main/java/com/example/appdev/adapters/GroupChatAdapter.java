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
        private TextView textViewMessage, textViewOriginalMessage, textViewSenderName;
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
            loadingDots = itemView.findViewById(R.id.loadingDots);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
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

            // Show sender name for received messages
            if (textViewSenderName != null) {
                textViewSenderName.setVisibility(showSenderInfo ? View.VISIBLE : View.GONE);
                if (showSenderInfo && message.getSenderName() != null) {
                    textViewSenderName.setText(message.getSenderName());
                }
            }

            if (currentUser != null && message.getSenderId().equals(currentUser.getUid())) {
                // Sent messages
                if (message.getMessageOG() != null) {
                    textViewMessage.setText(message.getMessageOG());
                    
                    // Remove click listener - we don't want to toggle between translations for sent messages
                    textViewMessage.setOnClickListener(null);
                } else {
                    textViewMessage.setText(message.getMessage());
                }
            } else {
                // Received messages
                // Handle loading state for messages being translated
                if (message.getMessage() != null && message.getMessage().equals("......")) {
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
                    String userLanguage = Variables.userLanguage;
                    if (message.getTranslations() != null && message.getTranslations().containsKey(userLanguage)) {
                        // Show message in user's language
                        String translatedText = message.getTranslations().get(userLanguage);
                        textViewMessage.setText(translatedText);
                    } else {
                        // No translation available, show the message as is
                textViewMessage.setText(message.getMessage());
                    }

                    // Set click listener for received messages to show original
                    textViewMessage.setOnClickListener(v -> {
                        if (message.getMessageOG() != null && 
                            !textViewMessage.getText().toString().equals(message.getMessageOG())) {
                            // Toggle between translation and original
                            if (textViewOriginalMessage != null) {
                                if (textViewOriginalMessage.getVisibility() == View.VISIBLE) {
                                    textViewOriginalMessage.setVisibility(View.GONE);
                                } else {
                                    textViewOriginalMessage.setVisibility(View.VISIBLE);
                                    textViewOriginalMessage.setText(message.getMessageOG());
                                }
                            }
                        }
                    });
                }

                // Change background color based on translation status
                if (messageCard != null) {
                    if (message.getMessage() != null && 
                        message.getMessageOG() != null && 
                        message.getMessage().equals(message.getMessageOG())) {
                        // Message is not translated - use light gray
                        messageCard.setCardBackgroundColor(itemView.getContext()
                            .getResources().getColor(R.color.light_gray));
                    } else {
                        // Message is translated - keep the default blue color from layout
                        // Do not override the color here
                    }
                }

                // Set the original message text if available
                if (textViewOriginalMessage != null) {
                    if (message.getMessageOG() != null && 
                        !message.getMessage().equals(message.getMessageOG())) {
                        textViewOriginalMessage.setVisibility(View.GONE);
                    } else {
                        textViewOriginalMessage.setVisibility(View.GONE);
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
            if (textViewOriginalMessage != null && message.getMessageOG() != null) {
                textViewOriginalMessage.setVisibility(View.VISIBLE);
                textViewOriginalMessage.setText(message.getMessageOG());
                textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
                visibleOriginalMessages.add(textViewOriginalMessage);
            }
        }

        private void showPopupMenu(View view, GroupMessage message) {
            PopupMenu popupMenu = new PopupMenu(context, view);
            popupMenu.inflate(R.menu.chat_message_menu);

            // Get menu items
            boolean isUntranslated = message.getMessage() != null && 
                                    message.getMessageOG() != null && 
                                    message.getMessage().equals(message.getMessageOG());

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
                        // Update the message in Firebase to the original message and remove variations
                        DatabaseReference messageRef = messagesRef.child(groupId)
                                .child(message.getMessageId());
                        messageRef.child("message").setValue(message.getMessageOG());
                        messageRef.child("messageVar1").removeValue();
                        messageRef.child("messageVar2").removeValue();
                        messageRef.child("messageVar3").removeValue();
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

            // Check if we have the original message
            if (message.getMessageOG() == null) {
                Toast.makeText(itemView.getContext(), 
                    "Cannot translate without original message", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            textViewMessage.setVisibility(View.GONE);
            if (loadingDots != null) {
                loadingDots.setVisibility(View.VISIBLE);
                loadingDots.startAnimation();
            }

            // Get source language from the message
            messagesRef.child(groupId).child(messageId).child("sourceLanguage")
                .get().addOnCompleteListener(task -> {
                    String sourceLanguage;
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getValue() != null) {
                        sourceLanguage = task.getResult().getValue(String.class);
                    } else {
                        sourceLanguage = "auto"; // Default to auto if not found
                    }

                    // Get the current user's language
                    String targetLanguage = Variables.userLanguage;

                    // If the source and target languages match, no need to translate
                    if (sourceLanguage.equals(targetLanguage)) {
                        // Just use the original message
                        messagesRef.child(groupId).child(messageId)
                            .child("translations")
                            .child(targetLanguage)
                            .setValue(message.getMessageOG());
                            
                        // Hide loading indicator and show message again
                        if (loadingDots != null) {
                            loadingDots.setVisibility(View.GONE);
                            loadingDots.stopAnimation();
                        }
                        textViewMessage.setVisibility(View.VISIBLE);
                        textViewMessage.setText(message.getMessageOG());
                        return;
                    }

                    // Prepare the request body
                    JSONObject requestBody = new JSONObject();
                    try {
                        requestBody.put("text", message.getMessageOG());
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
                });
        }
    }
}
