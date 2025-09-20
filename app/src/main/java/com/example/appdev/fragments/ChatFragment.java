package com.example.appdev.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.R;
import com.example.appdev.Variables;
import com.example.appdev.adapters.ChatItemAdapter;
import com.example.appdev.models.ChatItem;
import com.example.appdev.models.Group;
import com.example.appdev.models.User;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Pair;
import androidx.appcompat.widget.PopupMenu;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerViewUsers;
    private ChatItemAdapter chatItemAdapter;
    private List<ChatItem> chatItemList;
    private TextView emptyStateText;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;
    private DatabaseReference groupsRef;
    private DatabaseReference groupMessagesRef;
    private ValueEventListener messagesValueEventListener;
    private ValueEventListener usersValueEventListener;
    private ValueEventListener groupsValueEventListener;
    private ValueEventListener groupMessagesValueEventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize chat list and adapter
        chatItemList = new ArrayList<>();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            chatItemAdapter = new ChatItemAdapter(chatItemList, requireContext(), currentUser.getUid(), 
                (view, chatItem) -> {
                    // Show popup menu when three dots is clicked
                    showPopupMenu(view, chatItem);
                });
        } else {
            chatItemAdapter = new ChatItemAdapter(chatItemList, requireContext(), "", 
                (view, chatItem) -> {
                    // Show popup menu when three dots is clicked
                    showPopupMenu(view, chatItem);
                });
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);
        androidx.appcompat.widget.SearchView searchViewUsers = view.findViewById(R.id.searchViewUsers);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        View buttonGroupChat = view.findViewById(R.id.buttonGroupChat);
        View buttonSearchUsers = view.findViewById(R.id.buttonSearchUsers);

        // Initialize RecyclerView
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewUsers.setAdapter(chatItemAdapter);
        
        // Customize SearchView
        searchViewUsers.setQueryHint("Search chats...");
        
        // Add listener to SearchView
        searchViewUsers.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    // If search is empty, show all chats
                    loadAllChats();
                } else {
                    // Search in database
                    searchChats(newText.toLowerCase());
                }
                return false;
            }
        });


        // Set up search users button click listener
        buttonSearchUsers.setOnClickListener(v -> {
            // Make sure user is not a guest user
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            boolean isGuestUser = "guest".equals(currentUserId);

            if (isGuestUser) {
                CustomNotification.showNotification(requireContext(),
                        "You need to be logged in to search users", false);
                return;
            }

            // Navigate to SearchUsersActivity
            startActivity(new Intent(requireContext(), com.example.appdev.SearchUsersActivity.class));
        });

        // Set up group chat button click listener
        buttonGroupChat.setOnClickListener(v -> {
            // Make sure user is not a guest user
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            boolean isGuestUser = "guest".equals(currentUserId);

            if (isGuestUser) {
                CustomNotification.showNotification(requireContext(),
                        "You need to be logged in to use group chats", false);
                return;
            }

            // Navigate to GroupListActivity
            startActivity(new Intent(requireContext(), com.example.appdev.GroupListActivity.class));
        });

        // Get chats from Firebase
        loadAllChats();
    }

    private void loadAllChats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Handle case when user is not authenticated
            if (emptyStateText != null) {
                emptyStateText.setText("Please log in to view your chats");
                emptyStateText.setVisibility(View.VISIBLE);
            }
            if (recyclerViewUsers != null) {
                recyclerViewUsers.setVisibility(View.GONE);
            }
            return;
        }
        
        String currentUserId = currentUser.getUid();
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        groupMessagesRef = FirebaseDatabase.getInstance().getReference("group_messages");

        // Clear the existing list
        chatItemList.clear();
        
        // Load direct chats
        loadDirectChats(currentUserId);
        
        // Load group chats
        loadGroupChats(currentUserId);
    }
    
    private void loadDirectChats(String currentUserId) {
        // Handle direct chats (one-on-one)
        messagesValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Pair<String, Long>> userLastMessageInfo = new HashMap<>();
                
                // Loop through all chat rooms
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    String roomId = chatSnapshot.getKey();
                    if (roomId != null) {
                        String[] userIds = roomId.split("_");
                        if (userIds.length == 2) {
                            String otherUserId = userIds[0].equals(currentUserId) ? userIds[1] : 
                                (userIds[1].equals(currentUserId) ? userIds[0] : null);
                            
                            if (otherUserId != null) {
                                // Find the latest message timestamp for this chat
                                long latestTimestamp = 0;
                                String lastMessage = "";
                                String lastMessageOG = "";
                                String lastMessageSenderId = "";
                                for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                                    if (timestamp != null && timestamp > latestTimestamp) {
                                        latestTimestamp = timestamp;
                                        lastMessage = messageSnapshot.child("message").getValue(String.class);
                                        lastMessageOG = messageSnapshot.child("messageOG").getValue(String.class);
                                        lastMessageSenderId = messageSnapshot.child("senderId").getValue(String.class);
                                    }
                                }
                                userLastMessageInfo.put(otherUserId, new Pair<>(
                                    lastMessageSenderId + "|" + lastMessage + "|" + lastMessageOG, // Include messageOG
                                    latestTimestamp
                                ));
                            }
                        }
                    }
                }

                // Now get user details and create chat items
                usersValueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<ChatItem> directChats = new ArrayList<>();
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null && user.getUserId() != null && 
                                userLastMessageInfo.containsKey(user.getUserId()) && 
                                !user.getUserId().equals(currentUserId) && 
                                user.getEmail() != null) {
                                
                                Pair<String, Long> messageInfo = userLastMessageInfo.get(user.getUserId());
                                user.setLastMessage(messageInfo.first);
                                user.setLastMessageTime(messageInfo.second);
                                
                                // Create chat item from user
                                ChatItem chatItem = new ChatItem(user);
                                directChats.add(chatItem);
                            }
                        }

                        // Add direct chats to the chatItemList
                        synchronized (chatItemList) {
                            // Remove existing direct chats to avoid duplicates
                            chatItemList.removeIf(item -> !item.isGroup());
                            
                            // Add the direct chats
                            chatItemList.addAll(directChats);
                            
                            // Sort by timestamp (newest first)
                            sortAndUpdateList();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (isAdded() && getActivity() != null && 
                            FirebaseAuth.getInstance().getCurrentUser() != null) {
                            CustomNotification.showNotification(requireActivity(), 
                                "Failed to load users", false);
                        }
                    }
                };
                usersRef.addListenerForSingleValueEvent(usersValueEventListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isAdded() && getActivity() != null && 
                    FirebaseAuth.getInstance().getCurrentUser() != null) {
                    CustomNotification.showNotification(requireActivity(), 
                        "Failed to load chat rooms", false);
                }
            }
        };
        messagesRef.addValueEventListener(messagesValueEventListener);
    }
    
    private void loadGroupChats(String currentUserId) {
        // Create persistent value event listener for group messages
        groupMessagesValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Map to store group IDs to their last message info
                Map<String, GroupLastMessageInfo> groupLastMessageMap = new HashMap<>();
                
                // Process all group messages to find the latest one for each group
                for (DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                    String groupId = groupSnapshot.getKey();
                    if (groupId != null) {
                        long latestTimestamp = 0;
                        String lastMessage = "";
                        String lastMessageSenderId = "";
                        String lastMessageOG = "";
                        Map<String, String> lastMessageTranslations = new HashMap<>();
                        
                        // Loop through all messages in the group
                        for (DataSnapshot messageSnapshot : groupSnapshot.getChildren()) {
                            Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                            if (timestamp != null && timestamp > latestTimestamp) {
                                latestTimestamp = timestamp;
                                lastMessage = messageSnapshot.child("message").getValue(String.class);
                                lastMessageSenderId = messageSnapshot.child("senderId").getValue(String.class);
                                lastMessageOG = messageSnapshot.child("messageOG").getValue(String.class);
                                
                                // Get translations if available
                                DataSnapshot translationsSnapshot = messageSnapshot.child("translations");
                                if (translationsSnapshot.exists()) {
                                    for (DataSnapshot translationSnapshot : translationsSnapshot.getChildren()) {
                                        String language = translationSnapshot.getKey();
                                        String translatedText = translationSnapshot.getValue(String.class);
                                        if (language != null && translatedText != null) {
                                            lastMessageTranslations.put(language, translatedText);
                                        }
                                    }
                                }
                            }
                        }
                        
                        // If we found messages, store the info
                        if (latestTimestamp > 0) {
                            groupLastMessageMap.put(groupId, new GroupLastMessageInfo(
                                lastMessage,
                                lastMessageSenderId,
                                latestTimestamp,
                                lastMessageOG,
                                lastMessageTranslations
                            ));
                        }
                    }
                }
                
                // Now get group details and create chat items with the last message info
                groupsValueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<ChatItem> groupChats = new ArrayList<>();
                        
                        for (DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                            Group group = groupSnapshot.getValue(Group.class);
                            String groupId = groupSnapshot.getKey();
                            
                            // Check if current user is a member of this group
                            if (group != null && groupId != null && group.getMembers() != null && 
                                    group.getMembers().containsKey(currentUserId)) {
                                
                                // Update the group with last message info if available
                                GroupLastMessageInfo lastMessageInfo = groupLastMessageMap.get(groupId);
                                if (lastMessageInfo != null) {
                                    // Check if there's a translation for user's language
                                    String userLanguage = Variables.userLanguage;
                                    String displayMessage = lastMessageInfo.getMessage();
                                    
                                    // Get translation for user's language if the message is not from current user
                                    if (!currentUserId.equals(lastMessageInfo.getSenderId()) && 
                                        lastMessageInfo.getTranslations() != null && 
                                        lastMessageInfo.getTranslations().containsKey(userLanguage)) {
                                        displayMessage = lastMessageInfo.getTranslations().get(userLanguage);
                                    }
                                    
                                    group.setLastMessage(displayMessage);
                                    group.setLastMessageSenderId(lastMessageInfo.getSenderId());
                                    group.setLastMessageTime(lastMessageInfo.getTimestamp());
                                    group.setLastMessageOG(lastMessageInfo.getMessageOG());
                                } else {
                                    // No messages yet
                                    group.setLastMessage("No messages yet");
                                    group.setLastMessageSenderId("");
                                    group.setLastMessageTime(group.getCreatedAt()); // Use creation time for sorting
                                    group.setLastMessageOG("");
                                }
                                
                                // Create chat item from group
                                ChatItem chatItem = new ChatItem(group);
                                groupChats.add(chatItem);
                            }
                        }
                        
                        // Add group chats to the chatItemList
                        synchronized (chatItemList) {
                            // Remove existing group chats to avoid duplicates
                            chatItemList.removeIf(ChatItem::isGroup);
                            
                            // Add the group chats
                            chatItemList.addAll(groupChats);
                            
                            // Sort by timestamp (newest first)
                            sortAndUpdateList();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (isAdded() && getActivity() != null && 
                            FirebaseAuth.getInstance().getCurrentUser() != null) {
                            CustomNotification.showNotification(requireActivity(), 
                                "Failed to load group chats", false);
                        }
                    }
                };
                groupsRef.addValueEventListener(groupsValueEventListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isAdded() && getActivity() != null && 
                    FirebaseAuth.getInstance().getCurrentUser() != null) {
                    CustomNotification.showNotification(requireActivity(), 
                        "Failed to load group messages", false);
                }
            }
        };
        
        // Add persistent listener for group messages that will update when new messages arrive
        groupMessagesRef.addValueEventListener(groupMessagesValueEventListener);
    }
    
    private void sortAndUpdateList() {
        // Sort chats by last message time (newest first)
        Collections.sort(chatItemList, (item1, item2) -> 
            Long.compare(item2.getLastMessageTime(), item1.getLastMessageTime()));
        
        // Update UI
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                emptyStateText.setVisibility(chatItemList.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerViewUsers.setVisibility(chatItemList.isEmpty() ? View.GONE : View.VISIBLE);
                
                if (chatItemList.isEmpty()) {
                    emptyStateText.setText("No conversations yet\nStart chatting with someone!");
                }
                
                chatItemAdapter.notifyDataSetChanged();
            });
        }
    }

    private void searchChats(String searchText) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Handle case when user is not authenticated
            if (emptyStateText != null) {
                emptyStateText.setText("Please log in to view your chats");
                emptyStateText.setVisibility(View.VISIBLE);
            }
            if (recyclerViewUsers != null) {
                recyclerViewUsers.setVisibility(View.GONE);
            }
            return;
        }
        
        String currentUserId = currentUser.getUid();
        
        // Use the class-level references
        if (messagesRef == null) {
            messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        }
        
        if (usersRef == null) {
            usersRef = FirebaseDatabase.getInstance().getReference("users");
        }
        
        if (groupsRef == null) {
            groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        }

        // First get users with message history
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> userIdsWithMessages = new HashSet<>();
                
                // Get all user IDs with message history
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    String roomId = chatSnapshot.getKey();
                    if (roomId != null) {
                        String[] userIds = roomId.split("_");
                        if (userIds.length == 2) {
                            if (userIds[0].equals(currentUserId)) {
                                userIdsWithMessages.add(userIds[1]);
                            } else if (userIds[1].equals(currentUserId)) {
                                userIdsWithMessages.add(userIds[0]);
                            }
                        }
                    }
                }

                // Now search in users
                usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        chatItemList.clear();
                        List<ChatItem> userSearchResults = new ArrayList<>();
                        List<ChatItem> userMessageHistoryResults = new ArrayList<>();

                        // Search users
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null && user.getUserId() != null && 
                                !user.getUserId().equals(currentUserId) && 
                                user.getEmail() != null) {
                                
                                boolean matchesSearch = (user.getUsername() != null && 
                                    user.getUsername().toLowerCase().contains(searchText)) ||
                                    user.getEmail().toLowerCase().contains(searchText);
                                
                                if (matchesSearch) {
                                    // Create chat item
                                    ChatItem chatItem = new ChatItem(user);
                                    
                                    if (userIdsWithMessages.contains(user.getUserId())) {
                                        // Users with message history appear first
                                        userMessageHistoryResults.add(chatItem);
                                    } else {
                                        // Users without message history appear later
                                        userSearchResults.add(chatItem);
                                    }
                                }
                            }
                        }
                        
                        // Search groups
                        groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                List<ChatItem> groupSearchResults = new ArrayList<>();
                                
                                for (DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                                    Group group = groupSnapshot.getValue(Group.class);
                                    
                                    // Check if current user is a member and name matches search
                                    if (group != null && group.getMembers() != null && 
                                            group.getMembers().containsKey(currentUserId) &&
                                            group.getName() != null &&
                                            group.getName().toLowerCase().contains(searchText)) {
                                        
                                        // Create chat item from group
                                        ChatItem chatItem = new ChatItem(group);
                                        groupSearchResults.add(chatItem);
                                    }
                                }
                                
                                // Combine results: message history users, groups, then other users
                                chatItemList.addAll(userMessageHistoryResults);
                                chatItemList.addAll(groupSearchResults);
                                chatItemList.addAll(userSearchResults);
                                
                                // Update UI
                                emptyStateText.setVisibility(chatItemList.isEmpty() ? View.VISIBLE : View.GONE);
                                recyclerViewUsers.setVisibility(chatItemList.isEmpty() ? View.GONE : View.VISIBLE);
                                
                                if (chatItemList.isEmpty()) {
                                    emptyStateText.setText("No chats found");
                                }
                                
                                chatItemAdapter.notifyDataSetChanged();
                            }
                            
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Handle error
                                if (isAdded() && getActivity() != null) {
                                    CustomNotification.showNotification(requireActivity(), 
                                        "Failed to search groups", false);
                                }
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (isAdded() && getActivity() != null) {
                            CustomNotification.showNotification(requireActivity(), 
                                "Failed to search users", false);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isAdded() && getActivity() != null) {
                    CustomNotification.showNotification(requireActivity(), 
                        "Failed to search messages", false);
                }
            }
        });
    }

    private void showPopupMenu(View view, ChatItem chatItem) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        
        if (chatItem.isGroup()) {
            // Group chat options
            popup.inflate(R.menu.chat_group_context_menu);
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_view_group_info) {
                    // Open group info
                    Intent intent = new Intent(requireContext(), com.example.appdev.GroupInfoActivity.class);
                    intent.putExtra("groupId", chatItem.getId());
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        } else {
            // Direct chat options
            popup.inflate(R.menu.chat_user_context_menu);
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_view_profile) {
                    // Show user profile
                    showUserProfile(chatItem);
                    return true;
                } else if (itemId == R.id.action_delete_chat) {
                    // Delete conversation
                    deleteConversation(chatItem);
                    return true;
                }
                return false;
            });
        }
        
        popup.show();
    }

    private void showUserProfile(ChatItem chatItem) {
        // Create a bottom sheet dialog to show user profile
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(
            R.layout.user_profile_bottom_sheet, null);
        
        // Initialize views
        de.hdodenhof.circleimageview.CircleImageView profileImage = bottomSheetView.findViewById(R.id.profileImage);
        TextView username = bottomSheetView.findViewById(R.id.username);
        TextView email = bottomSheetView.findViewById(R.id.email);
        TextView language = bottomSheetView.findViewById(R.id.language);
        
        // Set user data (need to fetch full user data from Firebase)
        usersRef.child(chatItem.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    username.setText(user.getUsername());
                    email.setText(user.getEmail());
                    language.setText("Language: " + user.getLanguage());
                    
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none")) {
                        com.bumptech.glide.Glide.with(requireContext())
                            .load(user.getProfileImageUrl())
                            .placeholder(R.drawable.default_userpic)
                            .into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                CustomNotification.showNotification(requireContext(), 
                    "Failed to load user profile", false);
            }
        });
        
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void deleteConversation(ChatItem chatItem) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Conversation")
            .setMessage("Are you sure you want to delete this conversation? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                // Get current user ID
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                
                // Create room ID
                String[] ids = {currentUserId, chatItem.getId()};
                java.util.Arrays.sort(ids);
                String roomId = ids[0] + "_" + ids[1];
                
                // Delete conversation
                FirebaseDatabase.getInstance().getReference("messages")
                    .child(roomId)
                    .removeValue()
                    .addOnSuccessListener(aVoid -> {
                        CustomNotification.showNotification(requireActivity(), 
                            "Conversation deleted", true);
                        loadAllChats(); // Reload the chat list
                    })
                    .addOnFailureListener(e -> {
                        CustomNotification.showNotification(requireActivity(), 
                            "Failed to delete conversation", false);
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up listeners
        if (messagesRef != null && messagesValueEventListener != null) {
            messagesRef.removeEventListener(messagesValueEventListener);
        }
        
        if (usersRef != null && usersValueEventListener != null) {
            usersRef.removeEventListener(usersValueEventListener);
        }
        
        if (groupsRef != null && groupsValueEventListener != null) {
            groupsRef.removeEventListener(groupsValueEventListener);
        }
        
        // Also store reference to group messages listener for cleanup
        if (groupMessagesRef != null && groupMessagesValueEventListener != null) {
            groupMessagesRef.removeEventListener(groupMessagesValueEventListener);
        }
    }

    private static class GroupLastMessageInfo {
        private final String message;
        private final String senderId;
        private final long timestamp;
        private final String messageOG;
        private final Map<String, String> translations;

        public GroupLastMessageInfo(String message, String senderId, long timestamp) {
            this(message, senderId, timestamp, null, null);
        }
        
        public GroupLastMessageInfo(String message, String senderId, long timestamp, 
                                    String messageOG, Map<String, String> translations) {
            this.message = message;
            this.senderId = senderId;
            this.timestamp = timestamp;
            this.messageOG = messageOG;
            this.translations = translations;
        }

        public String getMessage() {
            return message;
        }

        public String getSenderId() {
            return senderId;
        }

        public long getTimestamp() {
            return timestamp;
        }
        
        public String getMessageOG() {
            return messageOG;
        }
        
        public Map<String, String> getTranslations() {
            return translations;
        }
    }
}
