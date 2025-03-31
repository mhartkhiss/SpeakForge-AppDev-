package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapters.GroupMemberAdapter;
import com.example.appdev.models.Group;
import com.example.appdev.models.User;
import com.example.appdev.utils.CustomNotification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupInfoActivity extends AppCompatActivity {

    private String groupId;
    private Group currentGroup;
    private GroupMemberAdapter adapter;
    private List<User> membersList;
    private TextView textViewGroupName, textViewGroupDescription, textViewMembersCount;
    private RecyclerView recyclerViewMembers;
    private Button buttonLeaveGroup;
    private FloatingActionButton fabAddMembers;
    private ValueEventListener adminStatusListener;
    private DatabaseReference userMemberRef;

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
                    fabAddMembers.setVisibility(View.VISIBLE);
                } else {
                    fabAddMembers.setVisibility(View.GONE);
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
        textViewGroupName = findViewById(R.id.textViewGroupName);
        textViewGroupDescription = findViewById(R.id.textViewGroupDescription);
        textViewMembersCount = findViewById(R.id.textViewMembersCount);
        recyclerViewMembers = findViewById(R.id.recyclerViewMembers);
        buttonLeaveGroup = findViewById(R.id.buttonLeaveGroup);
        fabAddMembers = findViewById(R.id.fabAddMembers);
        
        // Set up back button
        ImageView imageViewBack = findViewById(R.id.imageViewBack);
        imageViewBack.setOnClickListener(v -> finish());

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
        fabAddMembers.setOnClickListener(v -> {
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
                de.hdodenhof.circleimageview.CircleImageView imageViewGroupPic = 
                    findViewById(R.id.imageViewGroupPic);
                    
                if (currentGroup.getGroupImageUrl() != null && !currentGroup.getGroupImageUrl().isEmpty()) {
                    Glide.with(GroupInfoActivity.this)
                        .load(currentGroup.getGroupImageUrl())
                        .placeholder(R.drawable.group_default_icon)
                        .into(imageViewGroupPic);
                } else {
                    imageViewGroupPic.setImageResource(R.drawable.group_default_icon);
                }
                
                // Check if current user is admin and show/hide edit button
                ImageView imageViewEdit = findViewById(R.id.imageViewEdit);
                
                if (currentGroup.isAdmin(currentUserId)) {
                    imageViewEdit.setVisibility(View.VISIBLE);
                    imageViewEdit.setOnClickListener(v -> {
                        // Open edit group activity
                        Intent intent = new Intent(GroupInfoActivity.this, EditGroupActivity.class);
                        intent.putExtra("groupId", groupId);
                        startActivity(intent);
                    });
                } else {
                    imageViewEdit.setVisibility(View.GONE);
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
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    CustomNotification.showNotification(GroupInfoActivity.this, 
                        "Failed to load member info", false);
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
        if (adminStatusListener != null) {
            userMemberRef.removeEventListener(adminStatusListener);
        }
    }
}
