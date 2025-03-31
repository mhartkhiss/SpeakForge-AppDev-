package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.appdev.adapters.AddMembersAdapter;
import com.example.appdev.models.User;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddGroupMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewContacts;
    private EditText editTextSearch;
    private ProgressBar progressBar;
    private TextView textViewNoContacts;
    private Button buttonAdd;
    private ImageView imageViewBack;
    
    private AddMembersAdapter adapter;
    private List<User> allContacts;
    private List<User> filteredContacts;
    private HashMap<String, Boolean> selectedUsers;
    private String groupId;
    private List<String> currentMemberIds;
    private ValueEventListener membershipListener;
    private DatabaseReference groupMemberRef;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_members);
        
        // Get groupId from intent
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            CustomNotification.showNotification(this, "Error loading group details", false);
            finish();
            return;
        }
        
        // Initialize views
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        editTextSearch = findViewById(R.id.editTextSearch);
        progressBar = findViewById(R.id.progressBar);
        textViewNoContacts = findViewById(R.id.textViewNoContacts);
        buttonAdd = findViewById(R.id.buttonAdd);
        imageViewBack = findViewById(R.id.imageViewBack);
        
        // Setup collections
        allContacts = new ArrayList<>();
        filteredContacts = new ArrayList<>();
        selectedUsers = new HashMap<>();
        currentMemberIds = new ArrayList<>();
        
        // Setup RecyclerView
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddMembersAdapter(this, filteredContacts, selectedUsers);
        recyclerViewContacts.setAdapter(adapter);
        
        // Setup search functionality
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString().toLowerCase().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Setup back button
        imageViewBack.setOnClickListener(v -> finish());
        
        // Setup add button
        buttonAdd.setOnClickListener(v -> addMembersToGroup());
        buttonAdd.setEnabled(false);
        
        // Load current members first, then load potential contacts to add
        loadCurrentMembers();
        
        // Set up real-time listener to check if user is still a group member
        setupMembershipListener();
    }
    
    private void setupMembershipListener() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        groupMemberRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("members").child(currentUserId);
                
        membershipListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the user's entry is removed from the members list, close the activity
                if (!snapshot.exists()) {
                    CustomNotification.showNotification(AddGroupMembersActivity.this, 
                        "You are no longer a member of this group", false);
                    finish();
                    return;
                }
                
                // Check if user is still an admin, only admins can add members
                Boolean isAdmin = snapshot.getValue(Boolean.class);
                if (isAdmin == null || !isAdmin) {
                    CustomNotification.showNotification(AddGroupMembersActivity.this, 
                        "You no longer have admin privileges for this group", false);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if needed
            }
        };
        
        groupMemberRef.addValueEventListener(membershipListener);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener when activity is destroyed
        if (groupMemberRef != null && membershipListener != null) {
            groupMemberRef.removeEventListener(membershipListener);
        }
    }
    
    private void loadCurrentMembers() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Get current members of the group
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId);
                
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Check if current user is still a member of the group
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Map<String, Boolean> members = null;
                
                if (snapshot.child("members").exists()) {
                    for (DataSnapshot memberSnapshot : snapshot.child("members").getChildren()) {
                        if (members == null) {
                            members = new HashMap<>();
                        }
                        members.put(memberSnapshot.getKey(), memberSnapshot.getValue(Boolean.class));
                    }
                }
                
                if (members == null || !members.containsKey(currentUserId)) {
                    CustomNotification.showNotification(AddGroupMembersActivity.this, 
                        "You are no longer a member of this group", false);
                    finish();
                    return;
                }
                
                currentMemberIds.clear();
                
                for (DataSnapshot memberSnapshot : snapshot.child("members").getChildren()) {
                    currentMemberIds.add(memberSnapshot.getKey());
                }
                
                // Now load potential contacts to add
                loadContacts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                CustomNotification.showNotification(AddGroupMembersActivity.this, 
                    "Failed to load current members: " + error.getMessage(), false);
            }
        });
    }
    
    private void loadContacts() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allContacts.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    String userId = dataSnapshot.getKey();
                    
                    // Skip current user and users already in the group
                    if (user != null && userId != null && !userId.equals(currentUserId) 
                            && !currentMemberIds.contains(userId)) {
                        user.setUserId(userId);
                        allContacts.add(user);
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                
                if (allContacts.isEmpty()) {
                    textViewNoContacts.setText("No more contacts to add");
                    textViewNoContacts.setVisibility(View.VISIBLE);
                } else {
                    textViewNoContacts.setVisibility(View.GONE);
                    filteredContacts.addAll(allContacts);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                textViewNoContacts.setVisibility(View.VISIBLE);
                CustomNotification.showNotification(AddGroupMembersActivity.this, 
                    "Failed to load contacts: " + error.getMessage(), false);
            }
        });
        
        // Enable/disable add button based on selections
        adapter.setOnSelectionChangedListener(count -> {
            buttonAdd.setEnabled(count > 0);
            buttonAdd.setText(count > 0 ? "Add " + count + " Members" : "Add Members");
        });
    }
    
    private void filterContacts(String query) {
        filteredContacts.clear();
        
        if (query.isEmpty()) {
            filteredContacts.addAll(allContacts);
        } else {
            for (User user : allContacts) {
                if (user.getUsername() != null && user.getUsername().toLowerCase().contains(query) ||
                    user.getEmail() != null && user.getEmail().toLowerCase().contains(query)) {
                    filteredContacts.add(user);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        if (filteredContacts.isEmpty()) {
            textViewNoContacts.setText("No contacts found");
            textViewNoContacts.setVisibility(View.VISIBLE);
        } else {
            textViewNoContacts.setVisibility(View.GONE);
        }
    }
    
    private void addMembersToGroup() {
        if (selectedUsers.isEmpty()) {
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        buttonAdd.setEnabled(false);
        
        // Get database reference to the group members
        DatabaseReference groupMembersRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("members");
        
        // Create map of updates to perform
        Map<String, Object> updates = new HashMap<>();
        for (String userId : selectedUsers.keySet()) {
            updates.put(userId, false); // Add as regular member (not admin)
        }
        
        // Update the database
        groupMembersRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    int count = selectedUsers.size();
                    CustomNotification.showNotification(AddGroupMembersActivity.this, 
                        "Added " + count + " " + (count == 1 ? "member" : "members"), true);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonAdd.setEnabled(true);
                    CustomNotification.showNotification(AddGroupMembersActivity.this, 
                        "Failed to add members: " + e.getMessage(), false);
                });
    }
}
