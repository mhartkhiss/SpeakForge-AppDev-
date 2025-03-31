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

import com.example.appdev.models.Group;
import com.example.appdev.utils.CustomNotification;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateGroupActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    
    private EditText editTextGroupName;
    private EditText editTextGroupDescription;
    private Button buttonCreateGroup;
    private Button buttonAddMembers;
    private ImageView imageViewGroupPic;
    private ImageView imageViewBack;
    private ProgressBar progressBar;
    
    private Uri imageUri;
    private boolean hasSelectedImage = false;
    private Map<String, Boolean> selectedMembers;
    
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        
        // Initialize Firebase
        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        
        // Initialize views
        editTextGroupName = findViewById(R.id.editTextGroupName);
        editTextGroupDescription = findViewById(R.id.editTextGroupDescription);
        buttonCreateGroup = findViewById(R.id.buttonCreateGroup);
        buttonAddMembers = findViewById(R.id.buttonAddMembers);
        imageViewGroupPic = findViewById(R.id.imageViewGroupPic);
        imageViewBack = findViewById(R.id.imageViewBack);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize selected members map
        selectedMembers = new HashMap<>();
        // Add current user as admin by default
        selectedMembers.put(FirebaseAuth.getInstance().getCurrentUser().getUid(), true);
        
        // Set up back button
        imageViewBack.setOnClickListener(v -> finish());
        
        // Set up group pic selection
        imageViewGroupPic.setOnClickListener(v -> openImagePicker());
        
        // Set up add members button
        buttonAddMembers.setOnClickListener(v -> {
            Intent intent = new Intent(CreateGroupActivity.this, SelectGroupMembersActivity.class);
            intent.putExtra("selectedMembers", new HashMap<>(selectedMembers));
            startActivityForResult(intent, 2);
        });
        
        // Set up create group button
        buttonCreateGroup.setOnClickListener(v -> {
            if (validateInput()) {
                createGroup();
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
            hasSelectedImage = true;
        } else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            // Update selected members from SelectGroupMembersActivity
            HashMap<String, Boolean> updatedMembers = (HashMap<String, Boolean>) data.getSerializableExtra("selectedMembers");
            if (updatedMembers != null) {
                selectedMembers = updatedMembers;
                
                // Make sure current user is still admin
                selectedMembers.put(FirebaseAuth.getInstance().getCurrentUser().getUid(), true);
                
                // Update UI to show number of members
                int memberCount = selectedMembers.size();
                buttonAddMembers.setText(memberCount + " Members");
            }
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
    
    private void createGroup() {
        String groupName = editTextGroupName.getText().toString().trim();
        String groupDescription = editTextGroupDescription.getText().toString().trim();
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonCreateGroup.setEnabled(false);
        
        // Generate a unique group ID
        String groupId = FirebaseDatabase.getInstance().getReference("groups").push().getKey();
        
        if (hasSelectedImage && imageUri != null) {
            // Upload image first, then create group
            uploadImageAndCreateGroup(groupId, groupName, groupDescription);
        } else {
            // Create group without image
            createGroupInDatabase(groupId, groupName, groupDescription, null);
        }
    }
    
    private void uploadImageAndCreateGroup(String groupId, String groupName, String groupDescription) {
        String imageFileName = "group_images/" + UUID.randomUUID().toString();
        StorageReference fileRef = storageReference.child(imageFileName);
        
        fileRef.putFile(imageUri)
            .addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    createGroupInDatabase(groupId, groupName, groupDescription, imageUrl);
                });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                buttonCreateGroup.setEnabled(true);
                CustomNotification.showNotification(CreateGroupActivity.this, 
                    "Failed to upload image: " + e.getMessage(), false);
            });
    }
    
    private void createGroupInDatabase(String groupId, String groupName, String groupDescription, String imageUrl) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        long createdAt = System.currentTimeMillis();
        
        // Create group object
        Group group = new Group(groupId, groupName, groupDescription, createdAt, currentUserId);
        group.setMembers(selectedMembers);
        group.setGroupImageUrl(imageUrl);
        group.setDefaultLanguage(Variables.userLanguage);
        
        // Save to database
        databaseReference.child("groups").child(groupId).setValue(group)
            .addOnSuccessListener(aVoid -> {
                progressBar.setVisibility(View.GONE);
                
                // Launch the group chat
                Intent intent = new Intent(CreateGroupActivity.this, GroupChatActivity.class);
                intent.putExtra("groupId", groupId);
                intent.putExtra("groupName", groupName);
                intent.putExtra("groupImageUrl", imageUrl);
                startActivity(intent);
                
                // Close this activity
                finish();
                
                CustomNotification.showNotification(CreateGroupActivity.this, 
                    "Group created successfully", true);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                buttonCreateGroup.setEnabled(true);
                CustomNotification.showNotification(CreateGroupActivity.this, 
                    "Failed to create group: " + e.getMessage(), false);
            });
    }
}
