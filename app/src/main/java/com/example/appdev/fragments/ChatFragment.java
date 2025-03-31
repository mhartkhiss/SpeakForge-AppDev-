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
import com.example.appdev.adapters.UserAdapter;
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
    private UserAdapter userAdapter;
    private List<User> userList;
    private TextView emptyStateText;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;
    private ValueEventListener messagesValueEventListener;
    private ValueEventListener usersValueEventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize userList and userAdapter
        userList = new ArrayList<>();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userAdapter = new UserAdapter(userList, requireContext(), currentUser.getUid(), 
                (view, user) -> {
                    // Show popup menu when three dots is clicked
                    showPopupMenu(view, user);
                });
        } else {
            userAdapter = new UserAdapter(userList, requireContext(), "", 
                (view, user) -> {
                    // Show popup menu when three dots is clicked
                    showPopupMenu(view, user);
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

        // Initialize RecyclerView
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewUsers.setAdapter(userAdapter);
        
        // Customize SearchView
        searchViewUsers.setQueryHint("Search users...");
        
        // Add listener to SearchView
        searchViewUsers.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().isEmpty()) {
                    // If search is empty, show only users with message history
                    getUsersFromFirebase();
                } else {
                    // Search in database
                    searchUsers(newText.toLowerCase());
                }
                return false;
            }
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

        // Get users from Firebase
        getUsersFromFirebase();
    }

    private static class UserWithTimestamp {
        User user;
        long lastMessageTime;

        UserWithTimestamp(User user, long lastMessageTime) {
            this.user = user;
            this.lastMessageTime = lastMessageTime;
        }
    }

    private void getUsersFromFirebase() {
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

                // Now get user details and sort by timestamp
                usersValueEventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<UserWithTimestamp> usersWithTimestamp = new ArrayList<>();
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null && user.getUserId() != null && 
                                userLastMessageInfo.containsKey(user.getUserId()) && 
                                !user.getUserId().equals(currentUserId) && 
                                user.getEmail() != null) {
                                
                                Pair<String, Long> messageInfo = userLastMessageInfo.get(user.getUserId());
                                user.setLastMessage(messageInfo.first);
                                user.setLastMessageTime(messageInfo.second);
                                
                                usersWithTimestamp.add(new UserWithTimestamp(
                                    user, messageInfo.second
                                ));
                            }
                        }

                        // Sort by timestamp (newest first)
                        Collections.sort(usersWithTimestamp, (u1, u2) -> 
                            Long.compare(u2.lastMessageTime, u1.lastMessageTime));

                        // Update userList
                        userList.clear();
                        for (UserWithTimestamp uwt : usersWithTimestamp) {
                            userList.add(uwt.user);
                        }

                        // Update UI
                        emptyStateText.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerViewUsers.setVisibility(userList.isEmpty() ? View.GONE : View.VISIBLE);
                        
                        if (userList.isEmpty()) {
                            emptyStateText.setText("No conversations yet\nStart chatting with someone!");
                        }
                        
                        userAdapter.notifyDataSetChanged();
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

    private void searchUsers(String searchText) {
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

        // First get users with message history
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> userIdsWithMessages = new HashSet<>();
                
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
                        userList.clear();
                        List<User> searchResults = new ArrayList<>();
                        List<User> messageHistoryResults = new ArrayList<>();

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null && user.getUserId() != null && 
                                !user.getUserId().equals(currentUserId) && 
                                user.getEmail() != null) {
                                
                                boolean matchesSearch = (user.getUsername() != null && 
                                    user.getUsername().toLowerCase().contains(searchText)) ||
                                    user.getEmail().toLowerCase().contains(searchText);
                                
                                if (matchesSearch) {
                                    if (userIdsWithMessages.contains(user.getUserId())) {
                                        // Users with message history appear first
                                        messageHistoryResults.add(user);
                                    } else {
                                        // Users without message history appear last
                                        searchResults.add(user);
                                    }
                                }
                            }
                        }

                        // Combine results with message history users first
                        userList.addAll(messageHistoryResults);
                        userList.addAll(searchResults);

                        // Update UI
                        emptyStateText.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerViewUsers.setVisibility(userList.isEmpty() ? View.GONE : View.VISIBLE);
                        
                        if (userList.isEmpty()) {
                            emptyStateText.setText("No users found");
                        }
                        
                        userAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (isAdded() && getActivity() != null && 
                            FirebaseAuth.getInstance().getCurrentUser() != null) {
                            CustomNotification.showNotification(requireActivity(), 
                                "Failed to search users", false);
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isAdded() && getActivity() != null && 
                    FirebaseAuth.getInstance().getCurrentUser() != null) {
                    CustomNotification.showNotification(requireActivity(), 
                        "Failed to load chat rooms", false);
                }
            }
        });
    }

    private void showPopupMenu(View view, User user) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            CustomNotification.showNotification(requireActivity(), 
                "Please log in to perform this action", false);
            return;
        }
        
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.getMenuInflater().inflate(R.menu.chat_user_context_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_view_profile) {
                // Handle view profile action
                return true;
            } else if (itemId == R.id.action_delete_chat) {
                // Handle delete conversation action
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Remove any Firebase listeners if they exist
        if (messagesRef != null) {
            messagesRef.removeEventListener(messagesValueEventListener);
        }
        
        if (usersRef != null) {
            usersRef.removeEventListener(usersValueEventListener);
        }
    }
}
