package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.example.appdev.models.Group;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditGroupActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    
    private EditText editTextGroupName;
    private EditText editTextGroupDescription;
    private Button buttonSaveChanges;
    private ImageView imageViewGroupPic;
    private ImageView imageViewBack;
    private ProgressBar progressBar;
    
    private String groupId;
    private Group currentGroup;
    private Uri imageUri;
    private boolean hasSelectedNewImage = false;
    
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private ValueEventListener adminStatusListener;
    private DatabaseReference userMemberRef;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);
        
        // Get group ID from intent
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            CustomNotification.showNotification(this, "Error: Group not found", false);
            finish();
            return;
        }
        
        // Initialize Firebase
        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference("groups").child(groupId);
        userMemberRef = FirebaseDatabase.getInstance().getReference("groups").child(groupId).child("members").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        
        // Initialize views
        editTextGroupName = findViewById(R.id.editTextGroupName);
        editTextGroupDescription = findViewById(R.id.editTextGroupDescription);
        buttonSaveChanges = findViewById(R.id.buttonSaveChanges);
        imageViewGroupPic = findViewById(R.id.imageViewGroupPic);
        imageViewBack = findViewById(R.id.imageViewBack);
        progressBar = findViewById(R.id.progressBar);
        
        // Set up back button
        imageViewBack.setOnClickListener(v -> finish());
        
        // Set up group pic selection
        imageViewGroupPic.setOnClickListener(v -> openImagePicker());
        
        // Set up save button
        buttonSaveChanges.setOnClickListener(v -> {
            if (validateInput()) {
                saveGroupChanges();
            }
        });
        
        // Load current group data
        loadGroupData();
        
        // Add listener for admin status
        adminStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If user no longer exists in members or is no longer an admin
                if (!snapshot.exists()) {
                    CustomNotification.showNotification(EditGroupActivity.this, 
                        "You are no longer a member of this group", false);
                    finish();
                    return;
                }
                
                Boolean isAdmin = snapshot.getValue(Boolean.class);
                if (isAdmin == null || !isAdmin) {
                    CustomNotification.showNotification(EditGroupActivity.this, 
                        "You no longer have admin privileges for this group", false);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        };
        userMemberRef.addValueEventListener(adminStatusListener);
    }
    
    private void loadGroupData() {
        progressBar.setVisibility(View.VISIBLE);
        
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                
                currentGroup = snapshot.getValue(Group.class);
                if (currentGroup == null) {
                    CustomNotification.showNotification(EditGroupActivity.this, 
                        "Group not found", false);
                    finish();
                    return;
                }
                
                // Check if current user is still a member of the group
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (currentGroup.getMembers() == null || !currentGroup.getMembers().containsKey(currentUserId)) {
                    CustomNotification.showNotification(EditGroupActivity.this, 
                        "You are no longer a member of this group", false);
                    finish();
                    return;
                }
                
                // Fill the form with current data
                editTextGroupName.setText(currentGroup.getName());
                if (currentGroup.getDescription() != null) {
                    editTextGroupDescription.setText(currentGroup.getDescription());
                }
                
                // Load group image
                if (currentGroup.getGroupImageUrl() != null && !currentGroup.getGroupImageUrl().isEmpty()) {
                    Glide.with(EditGroupActivity.this)
                        .load(currentGroup.getGroupImageUrl())
                        .placeholder(R.drawable.group_default_icon)
                        .into(imageViewGroupPic);
                } else {
                    imageViewGroupPic.setImageResource(R.drawable.group_default_icon);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                CustomNotification.showNotification(EditGroupActivity.this, 
                    "Failed to load group data: " + error.getMessage(), false);
            }
        });
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageViewGroupPic.setImageURI(imageUri);
            hasSelectedNewImage = true;
        }
    }
    
    private boolean validateInput() {
        String groupName = editTextGroupName.getText().toString().trim();
        
        if (TextUtils.isEmpty(groupName)) {
            editTextGroupName.setError("Group name is required");
            return false;
        }
        
        return true;
    }
    
    private void saveGroupChanges() {
        String groupName = editTextGroupName.getText().toString().trim();
        String groupDescription = editTextGroupDescription.getText().toString().trim();
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonSaveChanges.setEnabled(false);
        
        if (hasSelectedNewImage && imageUri != null) {
            // Upload image first, then update group
            uploadImageAndUpdateGroup(groupName, groupDescription);
        } else {
            // Update group without changing image
            updateGroupInDatabase(groupName, groupDescription, currentGroup.getGroupImageUrl());
        }
    }
    
    private void uploadImageAndUpdateGroup(String groupName, String groupDescription) {
        String imageFileName = "group_images/" + UUID.randomUUID().toString();
        StorageReference fileRef = storageReference.child(imageFileName);
        
        fileRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    updateGroupInDatabase(groupName, groupDescription, imageUrl);
                });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                buttonSaveChanges.setEnabled(true);
                CustomNotification.showNotification(EditGroupActivity.this, 
                    "Failed to upload image: " + e.getMessage(), false);
            });
    }
    
    private void updateGroupInDatabase(String groupName, String groupDescription, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", groupName);
        updates.put("description", groupDescription);
        
        if (imageUrl != null) {
            updates.put("groupImageUrl", imageUrl);
        }
        
        databaseReference.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                CustomNotification.showNotification(EditGroupActivity.this, 
                    "Group updated successfully", true);
                finish();
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                buttonSaveChanges.setEnabled(true);
                CustomNotification.showNotification(EditGroupActivity.this, 
                    "Failed to update group: " + e.getMessage(), false);
            });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listener
        if (userMemberRef != null && adminStatusListener != null) {
            userMemberRef.removeEventListener(adminStatusListener);
        }
    }
}
