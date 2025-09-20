package com.example.appdev.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.GroupChatActivity;
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
    private Map<String, String> profileImageUrlCache = new HashMap<>();
    private DatabaseReference usersRef;
    private String visibleOriginalMessageId = null;
    private String regeneratingMessageId = null;
    private GroupMessage replyingToMessage = null;

    public GroupChatAdapter() {
        this.messages = new ArrayList<>();
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        setupUsernameCacheListener();
    }

    public GroupChatAdapter(DatabaseReference messagesRef, String groupId, Context context) {
        this.messagesRef = messagesRef;
        this.groupId = groupId;
        this.context = context;
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        messages = new ArrayList<>();
        setupUsernameCacheListener();
    }

    public void setVisibleOriginalMessageId(String messageId) {
        String oldVisibleId = this.visibleOriginalMessageId;
        this.visibleOriginalMessageId = messageId;

        // Use targeted notifications instead of notifyDataSetChanged()
        int oldPos = findPositionById(oldVisibleId);
        int newPos = findPositionById(messageId);

        if (oldPos != -1) {
            notifyItemChanged(oldPos);
        }
        if (newPos != -1 && newPos != oldPos) { // Avoid double notification if same item
            notifyItemChanged(newPos);
        }
    }

    public String getVisibleOriginalMessageId() {
        return visibleOriginalMessageId;
    }

    public String getRegeneratingMessageId() {
        return regeneratingMessageId;
    }

    private void setupUsernameCacheListener() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String username = userSnapshot.child("username").getValue(String.class);
                    String profileUrl = userSnapshot.child("profileImageUrl").getValue(String.class);
                    
                    if (username != null && !username.isEmpty()) {
                        usernameCache.put(userId, username);
                    }
                    
                    if (profileUrl != null && !profileUrl.isEmpty()) {
                        profileImageUrlCache.put(userId, profileUrl);
                    }
                }
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GroupChatAdapter", "Error loading user data: " + error.getMessage());
            }
        });
    }

    public void setMessages(List<GroupMessage> messages) {
        if (messages == null) {
            this.messages = new ArrayList<>();
        } else {
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
        return new GroupChatViewHolder(view, context, groupId, this);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupChatViewHolder holder, int position) {
        GroupMessage message = messages.get(position);
        
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
            return 1;
        }
        String senderId = message.getSenderId();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        return senderId.equals(currentUserId) ? 0 : 1;
    }

    // Helper method to find item position by ID
    private int findPositionById(String messageId) {
        if (messageId == null) return -1;
        for (int i = 0; i < messages.size(); i++) {
            GroupMessage msg = messages.get(i);
            if (msg != null && messageId.equals(msg.getMessageId())) {
                return i;
            }
        }
        return -1;
    }

    public static class GroupChatViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewMessage, textViewOriginalMessage;
        private TextView textViewSenderName;
        private TextView textViewReplySender, textViewReplyContent;
        private LinearLayout replyPreviewContainer;
        private LoadingDotsView loadingDots;
        private de.hdodenhof.circleimageview.CircleImageView imageViewProfile;
        private Context context;
        private CardView messageCard;
        private DatabaseReference messagesRef;
        private String groupId;
        private Map<String, String> usernameCache;
        private GroupChatAdapter adapter;

        public GroupChatViewHolder(@NonNull View itemView, Context context, String groupId, GroupChatAdapter adapter) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            textViewSenderName = itemView.findViewById(R.id.textViewSenderName);
            loadingDots = itemView.findViewById(R.id.loadingDots);
            imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
            replyPreviewContainer = itemView.findViewById(R.id.replyPreviewContainer);
            textViewReplySender = itemView.findViewById(R.id.textViewReplySender);
            textViewReplyContent = itemView.findViewById(R.id.textViewReplyContent);
            this.context = context;
            this.groupId = groupId;
            this.adapter = adapter;
            messageCard = itemView.findViewById(R.id.cardMessage);
            messagesRef = FirebaseDatabase.getInstance().getReference("group_messages");

            if (textViewMessage != null) {
                textViewMessage.setOnLongClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        GroupMessage message = (GroupMessage) textViewMessage.getTag();
                        if (message != null) {
                            showContextMenu(v, message);
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

            textViewMessage.setTag(message);
            
            String userLanguage = Variables.userLanguage;
            String originalLanguage = message.getSenderLanguage();
            Map<String, String> translations = message.getTranslations();
            boolean isCurrentUserMessage = currentUser != null && message.getSenderId().equals(currentUser.getUid());

            // --- State Check --- 
            boolean isRegenerating = message.getMessageId() != null &&
                                     message.getMessageId().equals(adapter.getRegeneratingMessageId());
                                     
            boolean shouldShowOriginal = !isRegenerating && // Don't show original if regenerating
                                         message.getMessageId() != null &&
                                         message.getMessageId().equals(adapter.getVisibleOriginalMessageId());

            // --- UI Setup based on State --- 

            // Handle Loading Indicator (if regenerating)
            if (loadingDots != null) {
                if (isRegenerating) {
                    loadingDots.setVisibility(View.VISIBLE);
                    loadingDots.startAnimation();
                } else {
                    loadingDots.setVisibility(View.GONE);
                    loadingDots.stopAnimation();
                }
            }
            
            // Handle reply preview if this is a reply message
            if (replyPreviewContainer != null) {
                if (message.isReply()) {
                    replyPreviewContainer.setVisibility(View.VISIBLE);
                    
                    // Set the original sender's name
                    String originalSenderId = message.getReplyToSenderId();
                    String originalSenderName = adapter.usernameCache.get(originalSenderId);
                    if (originalSenderName != null) {
                        textViewReplySender.setText(originalSenderName);
                    } else {
                        textViewReplySender.setText("User");
                    }
                    
                    // Set the original message content
                    String originalMessage = message.getReplyToMessage();
                    if (originalMessage != null) {
                        textViewReplyContent.setText(originalMessage);
                    } else {
                        textViewReplyContent.setText("Original message unavailable");
                    }
                    
                    // Set click listener to navigate to the original message
                    replyPreviewContainer.setOnClickListener(v -> {
                        String originalMessageId = message.getReplyToMessageId();
                        if (originalMessageId != null && !originalMessageId.isEmpty()) {
                            scrollToMessage(originalMessageId);
                        }
                    });
                } else {
                    replyPreviewContainer.setVisibility(View.GONE);
                }
            }

            // Handle Original Message View
            if (textViewOriginalMessage != null) { 
                if (shouldShowOriginal) {
                    String originalText = null;
                    if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                        originalText = translations.get(originalLanguage);
                    } else {
                        originalText = message.getMessage(); // Fallback
                    }

                    if (originalText != null) {
                        textViewOriginalMessage.setText(originalText);
                        textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
                        textViewOriginalMessage.setVisibility(View.VISIBLE);
                    } else {
                        textViewOriginalMessage.setVisibility(View.GONE);
                    }
                } else {
                     // Hide if not selected OR if regenerating
                    textViewOriginalMessage.setVisibility(View.GONE);
                }
            }

            // Handle Main Message Text View (hide if regenerating)
            if (textViewMessage != null) {
                 textViewMessage.setVisibility(isRegenerating ? View.GONE : View.VISIBLE);
                 if (!isRegenerating) { // Set text only if not regenerating
                    // Display logic for sent/received messages
                    if (isCurrentUserMessage) {
                        String originalText = null;
                        if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                            originalText = translations.get(originalLanguage);
                            textViewMessage.setText(originalText);
                            textViewMessage.setOnClickListener(null);
                        } else {
                            textViewMessage.setText(message.getMessage());
                        }
                    } else {
                        // Received message: Show translation or original based on availability
                        long currentTime = System.currentTimeMillis();
                        boolean isRecentMessage = (currentTime - message.getTimestamp()) < 5000;
                        boolean isTranslatingInitially = (translations == null) || (translations.isEmpty() && isRecentMessage);

                        if (isTranslatingInitially) {
                             // Show loading indicator for initial translation
                             textViewMessage.setVisibility(View.GONE);
                             if (loadingDots != null) {
                                 loadingDots.setVisibility(View.VISIBLE);
                                 loadingDots.startAnimation();
                             }
                        } else {
                             // Hide loading indicator if shown previously for initial translation
                             if (loadingDots != null) {
                                 loadingDots.setVisibility(View.GONE);
                                 loadingDots.stopAnimation();
                             }
                             textViewMessage.setVisibility(View.VISIBLE);
                            // Display actual message content (translation or original)
                            if (translations != null && translations.containsKey(userLanguage)) {
                                textViewMessage.setText(translations.get(userLanguage));
                            } else {
                                textViewMessage.setText(message.getMessage());
                            }
                        }
                        textViewMessage.setOnClickListener(v -> handleOriginalMessageClick(message));
                    }
                 } // end if(!isRegenerating)
             } // end if(textViewMessage != null)

            // Handle sender's profile image
            if (imageViewProfile != null) {
                imageViewProfile.setVisibility(showSenderInfo ? View.VISIBLE : View.INVISIBLE);
                if (showSenderInfo) {
                    String senderId = message.getSenderId();
                    String profileImageUrl = adapter.profileImageUrlCache.get(senderId);
                    
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(context)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.default_userpic)
                                .error(R.drawable.default_userpic)
                                .into(imageViewProfile);
                    } else if (message.getSenderProfileUrl() != null && !message.getSenderProfileUrl().isEmpty()) {
                        // Fallback to message's sender profile URL if available
                        Glide.with(context)
                                .load(message.getSenderProfileUrl())
                                .placeholder(R.drawable.default_userpic)
                                .error(R.drawable.default_userpic)
                                .into(imageViewProfile);
                    } else {
                        imageViewProfile.setImageResource(R.drawable.default_userpic);
                    }
                } else if (!showSenderInfo) {
                    Glide.with(context).clear(imageViewProfile);
                    imageViewProfile.setImageDrawable(null);
                }
            }

            if (textViewSenderName != null) {
                textViewSenderName.setVisibility(showSenderInfo ? View.VISIBLE : View.GONE);
                if (showSenderInfo) {
                    String senderId = message.getSenderId();
                    if (senderId != null && !senderId.isEmpty()) {
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
        }

        private void handleOriginalMessageClick(GroupMessage message) {
            String currentMessageId = message.getMessageId();
            if (currentMessageId == null) return; // Cannot toggle if message has no ID

            String currentlyVisibleId = adapter.getVisibleOriginalMessageId();

            if (currentMessageId.equals(currentlyVisibleId)) {
                // This message's original was visible, hide it
                adapter.setVisibleOriginalMessageId(null);
            } else {
                // A different (or no) message's original was visible, show this one
                adapter.setVisibleOriginalMessageId(currentMessageId);
            }
        }

        private void showContextMenu(View anchorView, GroupMessage message) {
            View popupView = LayoutInflater.from(context)
                    .inflate(R.layout.message_context_menu, null);

            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true // Focusable
            );

            popupWindow.setElevation(10); // Add shadow

            // Find the TextView items in the custom layout
            TextView replyItem = popupView.findViewById(R.id.menuItemReply);
            // Temporarily hide the reply option
            replyItem.setVisibility(View.GONE);
            
            TextView regenerateItem = popupView.findViewById(R.id.menuItemRegenerate);
            TextView toggleOriginalItem = popupView.findViewById(R.id.menuItemToggleOriginal);
            TextView removeTranslationItem = popupView.findViewById(R.id.menuItemRemoveTranslation);

            // --- Configure items based on GroupMessage state --- 
            String userLanguage = Variables.userLanguage;
            String senderLanguage = message.getSenderLanguage();
            Map<String, String> translations = message.getTranslations();
            
            // Reply functionality temporarily disabled
            replyItem.setOnClickListener(null);
            
            // Determine if the message is essentially untranslated for the current user
            boolean isUntranslated = senderLanguage != null && 
                                   userLanguage != null && 
                                   senderLanguage.equals(userLanguage);
            // Check if translations map is empty or null as another indicator
            boolean hasNoTranslations = translations == null || translations.isEmpty();
            // Refined check: Untranslated if languages match OR if there are simply no translations yet
            boolean showAsTranslate = isUntranslated || hasNoTranslations; 

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            boolean isUserMessage = currentUser != null && 
                                  message.getSenderId().equals(currentUser.getUid());

            // --- Visibility Logic --- 
            if (!isUserMessage) {
                // Hide remove option if it's not the user's own message
                removeTranslationItem.setVisibility(View.GONE);
            }

            if (showAsTranslate) {
                regenerateItem.setText("Translate");
                toggleOriginalItem.setVisibility(View.GONE); 
                // Also hide remove translation if it's untranslated
                removeTranslationItem.setVisibility(View.GONE); 
            } else {
                regenerateItem.setText("Regenerate Translation");
                toggleOriginalItem.setVisibility(View.VISIBLE);
                 removeTranslationItem.setVisibility(isUserMessage ? View.VISIBLE : View.GONE); // Show only if user's message
                
                // Update toggle text based on current state
                if (message.getMessageId() != null &&
                    message.getMessageId().equals(adapter.getVisibleOriginalMessageId())) {
                    toggleOriginalItem.setText("Hide Original Message");
                } else {
                    toggleOriginalItem.setText("Show Original Message");
                }
            }

            // --- Set Click Listeners --- 
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
                    String originalLanguage = message.getSenderLanguage();
                    String originalMessage = null;
                    
                    // Prioritize getting original text from the translations map if available
                    if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                        originalMessage = translations.get(originalLanguage);
                    } else {
                         // Fallback to the main message field if needed
                         // This might happen if translation failed or wasn't requested initially
                         originalMessage = message.getMessage(); 
                    }
                    
                    if (originalMessage != null) {
                        DatabaseReference messageRef = messagesRef.child(groupId)
                                .child(message.getMessageId());
                        
                        // Create map for multi-path update
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("message", originalMessage); // Set main message to original
                        updates.put("translations", null); // Remove entire translations node
                        
                        messageRef.updateChildren(updates).addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.e("GroupChatAdapter", "Failed to remove translations.", task.getException());
                                Toast.makeText(context, "Failed to remove translation", Toast.LENGTH_SHORT).show();
                            }
                            // UI will update via the main listener
                        });
                    }
                }
                popupWindow.dismiss();
            });

            // --- Show the popup --- 
            popupWindow.showAsDropDown(anchorView, 0, -anchorView.getHeight());
        }

        private void handleMessageTranslationClick(GroupMessage message) {
            int currentPosition = getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            String messageId = message.getMessageId();
            if (messageId == null) return; 
            
            // Hide any currently visible original message (state change only)
            adapter.visibleOriginalMessageId = null; // Directly set, don't notify yet
            // Set regenerating state
            adapter.regeneratingMessageId = messageId;
            // Notify only this item to update UI for loading state
            adapter.notifyItemChanged(currentPosition);

            if(Variables.userAccountType.equals("free")){
                Toast.makeText(itemView.getContext(), 
                    "You need to upgrade to regenerate translations", 
                    Toast.LENGTH_SHORT).show();
                adapter.regeneratingMessageId = null; // Clear state if not proceeding
                // Potentially notifyItemChanged again if needed
                return;
            }

            String originalLanguage = message.getSenderLanguage();
            Map<String, String> translations = message.getTranslations();
            String originalMessage = null;
            
            if (translations != null && originalLanguage != null && translations.containsKey(originalLanguage)) {
                originalMessage = translations.get(originalLanguage);
            } else {
                originalMessage = message.getMessage();
            }

            if (originalMessage == null) {
                Toast.makeText(itemView.getContext(), 
                    "Cannot translate without original message", 
                    Toast.LENGTH_SHORT).show();
                adapter.regeneratingMessageId = null; // Clear state on error
                adapter.notifyItemChanged(currentPosition); // Update UI
                return;
            }

            final String finalOriginalMessage = originalMessage;

            String sourceLanguage = message.getSenderLanguage();
            if (sourceLanguage == null) {
                sourceLanguage = "auto";
            }

            String targetLanguage = Variables.userLanguage;

            if (sourceLanguage.equals(targetLanguage)) {
                messagesRef.child(groupId).child(messageId)
                    .child("translations")
                    .child(targetLanguage)
                    .setValue(finalOriginalMessage)
                    .addOnCompleteListener(task -> {
                        adapter.regeneratingMessageId = null; // Clear state
                        adapter.notifyItemChanged(currentPosition); // Update UI
                    });
                return;
            }

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
                            
                            final String finalMessageId = messageId; // Capture messageId here

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
                                    
                                    // Always clear the regenerating state regardless of success or position finding
                                    adapter.regeneratingMessageId = null; 

                                    // Find the position using the captured messageId
                                    int finalPosition = adapter.findPositionById(finalMessageId); 

                                    if (finalPosition != RecyclerView.NO_POSITION) {
                                        adapter.notifyItemChanged(finalPosition); // Trigger bind to show result/error
                                    } else {
                                         Log.w("GroupChatAdapter", "Item position not found after regeneration for messageId: " + finalMessageId + ". Could not update UI.");
                                         // Consider notifyDataSetChanged() as a fallback if this happens often, but it's less efficient.
                                    }
                                    
                                    if (!success) {
                                        // Show toast only if the context is still valid
                                        if (itemView != null && itemView.getContext() != null) {
                                             Toast.makeText(itemView.getContext(), 
                                                 "Translation regeneration failed", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            }.execute(requestBody);

                        } catch (Exception e) {
                            Log.e("GroupChatAdapter", "Error preparing translation: " + e.getMessage());
                            // Clear state on exception before finding position
                            adapter.regeneratingMessageId = null; 
                            int position = adapter.findPositionById(messageId); // Use messageId here too
                            if (position != RecyclerView.NO_POSITION) {
                                adapter.notifyItemChanged(position); // Update UI
                            }
                        }
                    });

            } catch (Exception e) {
                Log.e("GroupChatAdapter", "Error preparing translation: " + e.getMessage());
                 // Clear state on exception before finding position
                adapter.regeneratingMessageId = null;
                int position = adapter.findPositionById(messageId); // Use messageId here too
                if (position != RecyclerView.NO_POSITION) {
                     adapter.notifyItemChanged(position); // Update UI
                }
            }
        }
        
        private void handleReplyClick(GroupMessage message) {
            // Store the message being replied to in the adapter
            adapter.replyingToMessage = message;
            
            // Notify the GroupChatActivity that we're replying to a message
            if (context instanceof GroupChatActivity) {
                ((GroupChatActivity) context).showReplyingToUI(message);
            }
        }
        
        private void scrollToMessage(String messageId) {
            int position = adapter.findPositionById(messageId);
            if (position != -1) {
                // Highlight the message briefly
                RecyclerView recyclerView = ((GroupChatActivity) context).getRecyclerView();
                if (recyclerView != null) {
                    recyclerView.scrollToPosition(position);
                    
                    // Get the view for the message and highlight it briefly
                    recyclerView.post(() -> {
                        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);
                        if (viewHolder != null && viewHolder.itemView != null) {
                            highlightView(viewHolder.itemView);
                        }
                    });
                }
            }
        }
        
        private void highlightView(View view) {
            // Save the original background color
            CardView cardView = view.findViewById(R.id.cardMessage);
            if (cardView != null) {
                int originalColor = cardView.getCardBackgroundColor().getDefaultColor();
                
                // Change to highlight color
                cardView.setCardBackgroundColor(Color.parseColor("#FFE082"));
                
                // Restore original color after delay
                new Handler().postDelayed(() -> {
                    cardView.setCardBackgroundColor(originalColor);
                }, 1000);
            }
        }
    }
    
    public GroupMessage getReplyingToMessage() {
        return replyingToMessage;
    }
    
    public void clearReplyingToMessage() {
        replyingToMessage = null;
    }
    
    /**
     * Gets the username for a given user ID from the cache
     * @param userId The user ID to look up
     * @return The username if found, null otherwise
     */
    public String getUsernameFromCache(String userId) {
        return usernameCache.get(userId);
    }
}
