package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapters.GroupMemberAdapter;
import com.example.appdev.models.Group;
import com.example.appdev.models.User;
import com.example.appdev.utils.CustomNotification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupInfoActivity extends AppCompatActivity {

    private String groupId;
    private Group currentGroup;
    private GroupMemberAdapter adapter;
    private List<User> membersList;
    private TextView textViewGroupName, textViewGroupDescription, textViewMembersCount;
    private CircleImageView imageViewGroupPic;
    private RecyclerView recyclerViewMembers;
    private MaterialButton buttonLeaveGroup, buttonAddMember;
    private ImageView imageViewBack, imageViewEdit;
    private ValueEventListener adminStatusListener;
    private DatabaseReference userMemberRef;
    private EditText editTextSearch;
    private ImageView imageViewClearSearch;
    private CardView cardViewProfile, cardViewMembersHeader, cardViewLeaveGroup, cardViewHeader;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        // Get group ID from intent
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            CustomNotification.showNotification(this, "Error loading group info", false);
            finish();
            return;
        }
        
        // Set up admin status check
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userMemberRef = FirebaseDatabase.getInstance().getReference("groups")
                .child(groupId).child("members").child(currentUserId);

        adminStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If user is no longer in the group
                if (!snapshot.exists()) {
                    CustomNotification.showNotification(GroupInfoActivity.this, 
                        "You are no longer a member of this group", false);
                    finish();
                    return;
                }
                
                // Check admin status - only admins can see the add members button
                Boolean isAdmin = snapshot.getValue(Boolean.class);
                if (isAdmin != null && isAdmin) {
                    buttonAddMember.setVisibility(View.VISIBLE);
                    imageViewEdit.setVisibility(View.VISIBLE);
                } else {
                    buttonAddMember.setVisibility(View.GONE);
                    imageViewEdit.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                CustomNotification.showNotification(GroupInfoActivity.this, 
                    "Failed to load admin status", false);
            }
        };
        userMemberRef.addValueEventListener(adminStatusListener);

        // Initialize views
        rootView = findViewById(R.id.constraintLayoutRoot);
        textViewGroupName = findViewById(R.id.textViewGroupName);
        textViewGroupDescription = findViewById(R.id.textViewGroupDescription);
        textViewMembersCount = findViewById(R.id.textViewMembersCount);
        imageViewGroupPic = findViewById(R.id.imageViewGroupPic);
        recyclerViewMembers = findViewById(R.id.recyclerViewMembers);
        buttonLeaveGroup = findViewById(R.id.buttonLeaveGroup);
        buttonAddMember = findViewById(R.id.buttonAddMember);
        imageViewBack = findViewById(R.id.imageViewBack);
        imageViewEdit = findViewById(R.id.imageViewEdit);
        cardViewProfile = findViewById(R.id.cardViewProfile);
        cardViewMembersHeader = findViewById(R.id.cardViewMembersHeader);
        cardViewLeaveGroup = findViewById(R.id.cardViewLeaveGroup);
        cardViewHeader = findViewById(R.id.cardViewHeader);
        
        // Initialize search views
        editTextSearch = findViewById(R.id.editTextSearch);
        imageViewClearSearch = findViewById(R.id.imageViewClearSearch);
        
        // Set up keyboard visibility listener
        setupKeyboardVisibilityListener();
        
        // Set up search functionality
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter members as user types
                adapter.filterMembers(s.toString());
                
                // Show/hide clear button based on text
                if (s.length() > 0) {
                    imageViewClearSearch.setVisibility(View.VISIBLE);
                } else {
                    imageViewClearSearch.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });
        
        // Set up clear search button
        imageViewClearSearch.setOnClickListener(v -> {
            editTextSearch.setText("");
            imageViewClearSearch.setVisibility(View.GONE);
            // Hide keyboard when clearing search
            hideKeyboard();
        });
        
        // Set up back button
        imageViewBack.setOnClickListener(v -> finish());

        // Set up edit button
        imageViewEdit.setOnClickListener(v -> {
            if (currentGroup != null) {
                Intent intent = new Intent(GroupInfoActivity.this, EditGroupActivity.class);
                intent.putExtra("groupId", groupId);
                startActivity(intent);
            }
        });

        // Set up recyclerview
        membersList = new ArrayList<>();
        adapter = new GroupMemberAdapter(this, membersList, groupId);
        recyclerViewMembers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMembers.setAdapter(adapter);

        // Load group info
        loadGroupInfo();

        // Set up leave group button
        buttonLeaveGroup.setOnClickListener(v -> confirmLeaveGroup());
        
        // Set up add members button
        buttonAddMember.setOnClickListener(v -> {
            if (currentGroup != null) {
                if (currentGroup.isAdmin(currentUserId)) {
                    // Open add members activity
                    Intent intent = new Intent(GroupInfoActivity.this, AddGroupMembersActivity.class);
                    intent.putExtra("groupId", groupId);
                    startActivity(intent);
                } else {
                    CustomNotification.showNotification(this, "Only admins can add members", false);
                }
            }
        });
        
        // Make "Done" on keyboard close it
        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }
    
    private void setupKeyboardVisibilityListener() {
        // Use a ViewTreeObserver to detect keyboard visibility changes
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            
            // If keyboard height is more than 15% of screen height, consider it visible
            boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;
            
            // Toggle UI elements visibility based on keyboard
            if (isKeyboardVisible) {
                // Hide profile, members header, and leave group button when keyboard is shown
                cardViewProfile.setVisibility(View.GONE);
                cardViewMembersHeader.setVisibility(View.GONE);
                cardViewLeaveGroup.setVisibility(View.GONE);
                
                // Make top header smaller in search mode
                cardViewHeader.setVisibility(View.GONE);
            } else {
                // Show all elements when keyboard is hidden
                cardViewProfile.setVisibility(View.VISIBLE);
                cardViewMembersHeader.setVisibility(View.VISIBLE);
                cardViewLeaveGroup.setVisibility(View.VISIBLE);
                
                // Restore the header
                cardViewHeader.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editTextSearch.getWindowToken(), 0);
        }
    }

    private void loadGroupInfo() {
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId);
        
        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentGroup = snapshot.getValue(Group.class);
                
                if (currentGroup == null) {
                    CustomNotification.showNotification(GroupInfoActivity.this, "Group not found", false);
                    finish();
                    return;
                }
                
                // Check if current user is still a member of the group
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (currentGroup.getMembers() == null || !currentGroup.getMembers().containsKey(currentUserId)) {
                    CustomNotification.showNotification(GroupInfoActivity.this, 
                        "You are no longer a member of this group", false);
                    finish();
                    return;
                }
                
                // Update UI with group details
                textViewGroupName.setText(currentGroup.getName());
                
                if (currentGroup.getDescription() != null && !currentGroup.getDescription().isEmpty()) {
                    textViewGroupDescription.setText(currentGroup.getDescription());
                    textViewGroupDescription.setVisibility(View.VISIBLE);
                } else {
                    textViewGroupDescription.setVisibility(View.GONE);
                }
                
                // Load group image
                if (currentGroup.getGroupImageUrl() != null && !currentGroup.getGroupImageUrl().isEmpty()) {
                    Glide.with(GroupInfoActivity.this)
                        .load(currentGroup.getGroupImageUrl())
                        .placeholder(R.drawable.group_default_icon)
                        .into(imageViewGroupPic);
                } else {
                    imageViewGroupPic.setImageResource(R.drawable.group_default_icon);
                }
                
                // Update members list
                if (currentGroup.getMembers() != null) {
                    loadGroupMembers(currentGroup.getMembers());
                    
                    // Update member count
                    int memberCount = currentGroup.getMembers().size();
                    textViewMembersCount.setText(memberCount + " " + 
                        (memberCount == 1 ? "Member" : "Members"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                CustomNotification.showNotification(GroupInfoActivity.this, 
                    "Failed to load group info", false);
            }
        });
    }
    
    private void loadGroupMembers(Map<String, Boolean> members) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        membersList.clear();
        
        // Handle the case when there are no members
        if (members.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }
        
        // Track how many members we need to load
        final int[] membersToLoad = {members.size()};
        
        for (Map.Entry<String, Boolean> member : members.entrySet()) {
            String userId = member.getKey();
            Boolean isAdmin = member.getValue();
            
            usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setUserId(userId);
                        user.setAdmin(isAdmin);
                        membersList.add(user);
                        
                        // Update the adapter with each new member
                        adapter.notifyDataSetChanged();
                    }
                    
                    // Decrement the counter
                    membersToLoad[0]--;
                    
                    // If all members have been loaded, update the original list for filtering
                    if (membersToLoad[0] == 0) {
                        adapter.updateOriginalList();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    CustomNotification.showNotification(GroupInfoActivity.this, 
                        "Failed to load member info", false);
                    
                    // Decrement the counter even on error
                    membersToLoad[0]--;
                    
                    // If all members have been processed (even with errors), update the original list
                    if (membersToLoad[0] == 0) {
                        adapter.updateOriginalList();
                    }
                }
            });
        }
    }
    
    private void confirmLeaveGroup() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Check if user is the only admin
        boolean isOnlyAdmin = false;
        if (currentGroup != null && currentGroup.getMembers() != null) {
            if (currentGroup.isAdmin(currentUserId)) {
                // Count other admins
                int adminCount = 0;
                for (Map.Entry<String, Boolean> member : currentGroup.getMembers().entrySet()) {
                    if (Boolean.TRUE.equals(member.getValue()) && !member.getKey().equals(currentUserId)) {
                        adminCount++;
                    }
                }
                isOnlyAdmin = adminCount == 0;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Leave Group");
        
        if (isOnlyAdmin && currentGroup.getMembers().size() > 1) {
            // If only admin and other members exist, suggest promoting someone else
            builder.setMessage("You're the only admin. Please promote another member to admin before leaving.");
            builder.setPositiveButton("OK", null);
        } else if (currentGroup.getMembers().size() == 1) {
            // If they're the only member, confirm deletion
            builder.setMessage("You're the only member. The group will be deleted if you leave. Continue?");
            builder.setPositiveButton("Delete Group", (dialog, which) -> deleteGroup());
            builder.setNegativeButton("Cancel", null);
        } else {
            // Normal leave operation
            builder.setMessage("Are you sure you want to leave this group?");
            builder.setPositiveButton("Leave", (dialog, which) -> leaveGroup());
            builder.setNegativeButton("Cancel", null);
        }
        
        builder.show();
    }
    
    private void leaveGroup() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId);
        
        groupRef.child("members").child(currentUserId).removeValue()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    CustomNotification.showNotification(this, "You left the group", true);
                    finish();
                } else {
                    CustomNotification.showNotification(this, "Failed to leave group", false);
                }
            });
    }
    
    private void deleteGroup() {
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId);
        DatabaseReference groupMessagesRef = FirebaseDatabase.getInstance().getReference("group_messages").child(groupId);
        
        // Delete group messages first
        groupMessagesRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Then delete the group itself
                groupRef.removeValue().addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        CustomNotification.showNotification(this, "Group deleted", true);
                        finish();
                    } else {
                        CustomNotification.showNotification(this, "Failed to delete group", false);
                    }
                });
            } else {
                CustomNotification.showNotification(this, "Failed to delete group messages", false);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adminStatusListener != null && userMemberRef != null) {
            userMemberRef.removeEventListener(adminStatusListener);
        }
    }
}
