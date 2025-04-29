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
import com.example.appdev.ChatActivity;
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
import java.util.HashMap;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Message> messages;
    private DatabaseReference messagesRef;
    private String roomId;
    private String visibleOriginalMessageId = null;
    private String regeneratingMessageId = null;
    private String cyclingMessageId = null;
    private Context context;
    private DatabaseReference usersRef;
    private Map<String, String> profileImageUrlCache = new HashMap<>();
    private Map<String, String> usernameCache = new HashMap<>();
    private Message replyingToMessage = null;

    public ChatAdapter() {
        this.messages = new ArrayList<>();
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        setupProfileImageCacheListener();
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
        this.usersRef = FirebaseDatabase.getInstance().getReference("users");
        setupProfileImageCacheListener();
    }

    public void setVisibleOriginalMessageId(String messageId) {
        String oldVisibleId = this.visibleOriginalMessageId;
        this.visibleOriginalMessageId = messageId;

        int oldPos = findPositionById(oldVisibleId);
        int newPos = findPositionById(messageId);

        if (oldPos != -1) {
            notifyItemChanged(oldPos);
        }
        if (newPos != -1 && newPos != oldPos) { 
            notifyItemChanged(newPos);
        } else if (newPos == -1 && messageId != null) {
            Log.w("ChatAdapter", "Could not find position for new visible messageId: " + messageId);
        }
    }

    public String getVisibleOriginalMessageId() {
        return visibleOriginalMessageId;
    }
    
    public String getRegeneratingMessageId() {
        return regeneratingMessageId;
    }

    public String getCyclingMessageId() {
        return cyclingMessageId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                viewType == 0 ? R.layout.item_message_sent : R.layout.item_message_received,
                parent, false);
        return new ChatViewHolder(view, messagesRef, roomId, this, context);
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

    private int findPositionById(String messageId) {
        if (messageId == null) return -1;
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (msg != null && messageId.equals(msg.getMessageId())) {
                return i;
            }
        }
        return -1;
    }

    private void setupProfileImageCacheListener() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                profileImageUrlCache.clear();
                usernameCache.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    String profileUrl = userSnapshot.child("profileImageUrl").getValue(String.class);
                    String username = userSnapshot.child("username").getValue(String.class);
                    
                    if (userId != null && profileUrl != null && !profileUrl.isEmpty()) {
                        profileImageUrlCache.put(userId, profileUrl);
                    }
                    
                    if (userId != null && username != null && !username.isEmpty()) {
                        usernameCache.put(userId, username);
                    }
                }
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatAdapter", "Error loading user data: " + error.getMessage());
            }
        });
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewMessage, textViewOriginalMessage;
        private TextView textViewReplySender, textViewReplyContent;
        private LinearLayout replyPreviewContainer;
        private LoadingDotsView loadingDots;
        private DatabaseReference messagesRef;
        private String roomId;
        private ChatAdapter adapter;
        private de.hdodenhof.circleimageview.CircleImageView imageViewProfile;
        private Context context;

        public ChatViewHolder(@NonNull View itemView, DatabaseReference messagesRef, String roomId, 
                ChatAdapter adapter, Context context) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.textViewMessage);
            textViewOriginalMessage = itemView.findViewById(R.id.textViewOriginalMessage);
            loadingDots = itemView.findViewById(R.id.loadingDots);
            replyPreviewContainer = itemView.findViewById(R.id.replyPreviewContainer);
            textViewReplySender = itemView.findViewById(R.id.textViewReplySender);
            textViewReplyContent = itemView.findViewById(R.id.textViewReplyContent);
            this.messagesRef = messagesRef;
            this.roomId = roomId;
            this.adapter = adapter;
            this.context = context;
            this.imageViewProfile = itemView.findViewById(R.id.imageViewProfile);
        }

        public void bind(Message message, boolean showAvatar) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            
            boolean isSentMessage = currentUser != null && message.getSenderId().equals(currentUser.getUid());

            // --- State Check ---
            boolean isRegenerating = message.getMessageId() != null &&
                                     message.getMessageId().equals(adapter.getRegeneratingMessageId());

            boolean shouldShowOriginal = !isRegenerating && // Don't show original if regenerating
                                         message.getMessageId() != null &&
                                         message.getMessageId().equals(adapter.getVisibleOriginalMessageId());
            
             // Get translation state for initial loading check (only relevant for received messages)
             String translationState = message.getTranslationState();
             // Only consider initial translation state for *received* messages
             boolean isInitialTranslation = !isSentMessage && "TRANSLATING".equals(translationState); 

             // Check cycling state from adapter
             boolean isCycling = message.getMessageId() != null && 
                                message.getMessageId().equals(adapter.getCyclingMessageId());

            // --- UI Setup based on State --- 
            
            // Determine if loading should be shown based on message type and adapter states
            boolean showLoading = isRegenerating || isInitialTranslation || isCycling; 

            // Handle Loading Indicator 
            if (loadingDots != null) {
                if (showLoading) { 
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

            // Handle Main Message Text View
            if (textViewMessage != null) {
                 // Hide main text if loading is active for this message type
                 textViewMessage.setVisibility(showLoading ? View.GONE : View.VISIBLE);
                 
                 // Set text and listeners only if not loading
                 if (!showLoading) { 
                    if (isSentMessage) {
                        // --- Sent message logic ---
                        // Sent messages always show original text
                        textViewMessage.setText(message.getMessage());
                        textViewMessage.setOnClickListener(null); 
                        textViewMessage.setOnLongClickListener(null);

                    } else {
                        // --- Received message logic ---
                        // Display translation or original based on availability
                        Map<String, String> translations = message.getTranslations();
                        // String translationState = message.getTranslationState(); // Already fetched above

                        // Use "REMOVED" state to decide if translation should be shown
                        if (translations != null && translations.containsKey("translation1") && 
                            !"REMOVED".equals(translationState)) {
                            textViewMessage.setText(translations.get("translation1"));
                        } else {
                            textViewMessage.setText(message.getMessage()); // Show original if no translation or removed
                        }
                        
                        // Always use the same background color regardless of translation status
                        // (Keep this specific to received messages if desired)
                        View cardView = (View) textViewMessage.getParent().getParent();
                        if (cardView instanceof CardView) {
                            CardView messageCard = (CardView) cardView;
                            messageCard.setCardBackgroundColor(itemView.getContext()
                                .getResources().getColor(R.color.message_received_bg));
                        }
                        
                        // Set click listeners for received messages
                        textViewMessage.setOnClickListener(v -> handleOriginalMessageClick(message));
                        textViewMessage.setOnLongClickListener(v -> {
                            showContextMenu(v, message);
                            return true;
                        });
                    }
                 } // end if(!showLoading)
             } // end if(textViewMessage != null)

            // Handle Original Message View
            if (textViewOriginalMessage != null) { 
                 // Show original only if requested state is true AND not loading (initial, regenerating, or cycling)
                if (shouldShowOriginal && !showLoading) { 
                    // Show original message text
                    textViewOriginalMessage.setText(message.getMessage());
                    textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.grey));
                    textViewOriginalMessage.setVisibility(View.VISIBLE);
                } else {
                    // Hide if not selected OR if loading (initial or regenerating)
                    textViewOriginalMessage.setVisibility(View.GONE);
                }
            }

            if (imageViewProfile != null) {
                int layoutType = getItemViewType();
                if (layoutType == 1) {
                    imageViewProfile.setVisibility(showAvatar ? View.VISIBLE : View.INVISIBLE);
                    if (showAvatar) {
                        String senderId = message.getSenderId();
                        String profileImageUrl = adapter.profileImageUrlCache.get(senderId);

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.default_userpic)
                                    .error(R.drawable.default_userpic)
                                    .into(imageViewProfile);
                        } else {
                            imageViewProfile.setImageResource(R.drawable.default_userpic);
                        }
                    } else if (!showAvatar) {
                        Glide.with(context).clear(imageViewProfile); 
                        imageViewProfile.setImageDrawable(null);
                    }
                } else {
                    imageViewProfile.setVisibility(View.GONE); 
                }
            }
        }

        private void handleOriginalMessageClick(Message message) {
            String currentMessageId = message.getMessageId();
            if (currentMessageId == null) return;

            if (Variables.userAccountType.equals("free")) {
                if (textViewOriginalMessage != null) {
                    textViewOriginalMessage.setVisibility(View.VISIBLE);
                    textViewOriginalMessage.setText("You can view the original message by upgrading to a premium account.");
                    textViewOriginalMessage.setTextColor(itemView.getResources().getColor(R.color.purple_200));
                    textViewOriginalMessage.postDelayed(() -> {
                        if (textViewOriginalMessage.getText().toString().startsWith("You can view")) {
                             textViewOriginalMessage.setVisibility(View.GONE);
                        }
                    }, 3000);
                }
                return;
            }

            String currentlyVisibleId = adapter.getVisibleOriginalMessageId();

            if (currentMessageId.equals(currentlyVisibleId)) {
                adapter.setVisibleOriginalMessageId(null);
            } else {
                adapter.setVisibleOriginalMessageId(currentMessageId);
            }
        }

        private void handleMessageTranslationClick(Message message) {
            int currentPosition = getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            String messageId = message.getMessageId();
            if (messageId == null) {
                Log.e("ChatAdapter", "Cannot handle translation click without message ID.");
                return;
            }
            
            // --- Check for existing variations for cycling --- 
            Map<String, String> translations = message.getTranslations();
            String translation1 = translations != null ? translations.get("translation1") : null;
            String translation2 = translations != null ? translations.get("translation2") : null;
            String translation3 = translations != null ? translations.get("translation3") : null;

            boolean hasAllVariations = translation1 != null && !translation1.isEmpty() &&
                                       translation2 != null && !translation2.isEmpty() &&
                                       translation3 != null && !translation3.isEmpty();

            if (hasAllVariations) {
                 // Perform rotational swap of translation values in Firebase
                String currentVal1 = translations.get("translation1");
                String currentVal2 = translations.get("translation2");
                String currentVal3 = translations.get("translation3");

                 // --- Update Firebase with rotated values --- 
                 DatabaseReference translationsRef = adapter.messagesRef.child(adapter.roomId).child(messageId).child("translations");
                
                 // Set adapter state and notify to show loading via bind()
                 adapter.cyclingMessageId = messageId;
                 adapter.notifyItemChanged(currentPosition);
                 
                 // Record start time for minimum duration calculation
                 long startTime = System.currentTimeMillis();

                // Prepare map for the rotational update
                Map<String, Object> updatesMap = new HashMap<>();
                updatesMap.put("translation1", currentVal2); // T1 gets old T2
                updatesMap.put("translation2", currentVal3); // T2 gets old T3
                updatesMap.put("translation3", currentVal1); // T3 gets old T1

                translationsRef.updateChildren(updatesMap)
                    .addOnCompleteListener(task -> {
                         // Calculate duration
                         long endTime = System.currentTimeMillis();
                         long duration = endTime - startTime;
                         long delayNeeded = 1000 - duration; // Delay needed to reach 1 second total
                         
                         // Define the final UI update action
                         Runnable finalUiUpdateRunnable = () -> {
                            // Check if still cycling this message before clearing state
                            if (messageId.equals(adapter.cyclingMessageId)) { 
                                adapter.cyclingMessageId = null; // Clear state
                            }
                            // Notify item changed to reflect final state (loading hidden)
                            // Find position again in case it changed
                            int finalPosition = adapter.findPositionById(messageId);
                            if (finalPosition != RecyclerView.NO_POSITION) {
                                 adapter.notifyItemChanged(finalPosition); 
                            } else {
                                 Log.w("ChatAdapter", "Item position not found after cycle completion for messageId: " + messageId);
                                 // May need notifyDataSetChanged() as fallback if positions change frequently
                            }
                         };

                         // Schedule or run the final update
                         if (delayNeeded > 0) {
                             itemView.postDelayed(finalUiUpdateRunnable, delayNeeded);
                         } else {
                             finalUiUpdateRunnable.run(); // Run immediately if >= 1 second passed
                         }
                         
                         if (!task.isSuccessful()) {
                             Log.e("ChatAdapter", "Failed to rotate translation variations.", task.getException());
                             if (context != null) {
                                 Toast.makeText(context, "Failed to cycle variation", Toast.LENGTH_SHORT).show();
                             }
                         }
                    });
                // --- End Firebase Update ---
                
                // Return: No API call needed, Firebase update initiated
                return; 
            }
            // --- End variation cycling check ---

             // Check for premium status (only needed if we proceed to API call)
             if (Variables.userAccountType.equals("free")) {
                Toast.makeText(itemView.getContext(), 
                    "You need to upgrade to regenerate translations", 
                    Toast.LENGTH_SHORT).show();
                return;
            }

            adapter.visibleOriginalMessageId = null;
            adapter.regeneratingMessageId = messageId;
            adapter.notifyItemChanged(currentPosition); 

            String originalMessage = message.getMessage();
            if (originalMessage == null || originalMessage.isEmpty()) {
                Log.e("ChatAdapter", "Cannot regenerate translation without original message text.");
                Toast.makeText(context, "Original message not found.", Toast.LENGTH_SHORT).show();
                adapter.regeneratingMessageId = null; 
                int errorPosition = adapter.findPositionById(messageId);
                if (errorPosition != RecyclerView.NO_POSITION) {
                   adapter.notifyItemChanged(errorPosition);
                }
                return;
            }
            
            String targetLanguage = Variables.userLanguage;
            
            RegenerateMessageTranslation regenerator = new RegenerateMessageTranslation(context, new RegenerateMessageTranslation.RegenerationCallback() {
                @Override
                public void onComplete(boolean success) {
                    final String completedMessageId = messageId; 
                    
                    adapter.regeneratingMessageId = null; 

                    int finalPosition = adapter.findPositionById(completedMessageId); 

                    if (finalPosition != RecyclerView.NO_POSITION) {
                        adapter.notifyItemChanged(finalPosition); 
                    } else {
                         Log.w("ChatAdapter", "Item position not found after regeneration for messageId: " + completedMessageId + ". Could not update UI.");
                    }
                    
                    if (!success) {
                        if (itemView != null && itemView.getContext() != null) {
                             Toast.makeText(itemView.getContext(), 
                                 "Translation regeneration failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            
            regenerator.regenerate(originalMessage, message.getMessageId(), targetLanguage);
        }

        private void showContextMenu(View anchor, Message message) {
            View popupView = LayoutInflater.from(anchor.getContext())
                    .inflate(R.layout.message_context_menu, null);
            
            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setElevation(10);

            TextView replyItem = popupView.findViewById(R.id.menuItemReply);
            TextView regenerateItem = popupView.findViewById(R.id.menuItemRegenerate);
            TextView toggleOriginalItem = popupView.findViewById(R.id.menuItemToggleOriginal);
            TextView removeTranslationItem = popupView.findViewById(R.id.menuItemRemoveTranslation);

            Map<String, String> translations = message.getTranslations();
            String translationState = message.getTranslationState();
            boolean hasVisibleTranslation = translations != null && 
                                         translations.containsKey("translation1") &&
                                         !"REMOVED".equals(translationState);

            // Check if all variations exist for potential cycling
            String translation1 = translations != null ? translations.get("translation1") : null;
            String translation2 = translations != null ? translations.get("translation2") : null;
            String translation3 = translations != null ? translations.get("translation3") : null;
            boolean hasAllVariations = translation1 != null && !translation1.isEmpty() &&
                                       translation2 != null && !translation2.isEmpty() &&
                                       translation3 != null && !translation3.isEmpty();

            if (!hasVisibleTranslation) {
                regenerateItem.setText("Translate");
                toggleOriginalItem.setVisibility(View.GONE);
                removeTranslationItem.setVisibility(View.GONE);
            } else {
                
                regenerateItem.setText("Regenerate Translation"); 
                toggleOriginalItem.setVisibility(View.VISIBLE); // Make sure it's visible
                removeTranslationItem.setVisibility(View.VISIBLE); // Make sure it's visible

                if (message.getMessageId() != null && 
                    message.getMessageId().equals(adapter.getVisibleOriginalMessageId())) {
                    toggleOriginalItem.setText("Hide Original Message");
                } else {
                    toggleOriginalItem.setText("Show Original Message");
                }
            }

            replyItem.setOnClickListener(v -> {
                handleReplyClick(message);
                popupWindow.dismiss();
            });
            
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
                    String messageId = message.getMessageId();
                    DatabaseReference messageRef = messagesRef.child(Variables.roomId)
                            .child(messageId);
                    
                    messageRef.child("translationState").setValue("REMOVED").addOnCompleteListener(task -> {
                         if(task.isSuccessful()){
                             messageRef.child("translations").removeValue(); 
                             
                             if(messageId.equals(adapter.getVisibleOriginalMessageId())){
                                 adapter.setVisibleOriginalMessageId(null); 
                             } else {
                                 int pos = adapter.findPositionById(messageId);
                                 if(pos != -1) {
                                     adapter.notifyItemChanged(pos);
                                 }
                             }
                         } else {
                              Log.e("ChatAdapter", "Failed to set translationState to REMOVED");
                              Toast.makeText(context, "Failed to remove translation.", Toast.LENGTH_SHORT).show();
                         }
                    });
                }
                popupWindow.dismiss();
            });

            popupWindow.showAsDropDown(anchor, 0, -anchor.getHeight());
        }
        
        private void handleReplyClick(Message message) {
            // Store the message being replied to in the adapter
            adapter.replyingToMessage = message;
            
            // Notify the ChatActivity that we're replying to a message
            if (context instanceof ChatActivity) {
                ((ChatActivity) context).showReplyingToUI(message);
            }
        }
        
        private void scrollToMessage(String messageId) {
            int position = adapter.findPositionById(messageId);
            if (position != -1) {
                // Highlight the message briefly
                RecyclerView recyclerView = ((ChatActivity) context).getRecyclerView();
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
                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE082"));
                
                // Restore original color after delay
                new android.os.Handler().postDelayed(() -> {
                    cardView.setCardBackgroundColor(originalColor);
                }, 1000);
            }
        }
    }
    
    public Message getReplyingToMessage() {
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
