package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.appdev.adapters.GroupListAdapter;
import com.example.appdev.models.Group;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewGroups;
    private ProgressBar progressBar;
    private TextView textViewNoGroups;
    private FloatingActionButton fabCreateGroup;
    private ImageView imageViewBack;
    
    private List<Group> userGroups;
    private GroupListAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);
        
        // Initialize views
        recyclerViewGroups = findViewById(R.id.recyclerViewGroups);
        progressBar = findViewById(R.id.progressBar);
        textViewNoGroups = findViewById(R.id.textViewNoGroups);
        fabCreateGroup = findViewById(R.id.fabCreateGroup);
        imageViewBack = findViewById(R.id.imageViewBack);
        
        // Set up recycler view
        userGroups = new ArrayList<>();
        adapter = new GroupListAdapter(this, userGroups);
        recyclerViewGroups.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGroups.setAdapter(adapter);
        
        // Set up item click listener
        adapter.setOnItemClickListener(group -> {
            // Open group chat
            Intent intent = new Intent(GroupListActivity.this, GroupChatActivity.class);
            intent.putExtra("groupId", group.getGroupId());
            intent.putExtra("groupName", group.getName());
            intent.putExtra("groupImageUrl", group.getGroupImageUrl());
            startActivity(intent);
        });
        
        // Set up back button
        imageViewBack.setOnClickListener(v -> finish());
        
        // Set up create group button
        fabCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(GroupListActivity.this, CreateGroupActivity.class);
            startActivity(intent);
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload groups when resuming activity
        loadUserGroups();
    }
    
    private void loadUserGroups() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoGroups.setVisibility(View.GONE);
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference groupsRef = FirebaseDatabase.getInstance().getReference("groups");
        
        groupsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userGroups.clear();
                
                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    Group group = groupSnapshot.getValue(Group.class);
                    
                    // Check if current user is a member of this group
                    if (group != null && group.getMembers() != null 
                            && group.getMembers().containsKey(currentUserId)) {
                        userGroups.add(group);
                    }
                }
                
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                
                if (userGroups.isEmpty()) {
                    textViewNoGroups.setVisibility(View.VISIBLE);
                } else {
                    textViewNoGroups.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                textViewNoGroups.setVisibility(View.VISIBLE);
                CustomNotification.showNotification(GroupListActivity.this, 
                    "Failed to load groups: " + error.getMessage(), false);
            }
        });
    }
}
