package com.example.appdev.controller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.appdev.fragments.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ChangeProfilePicControl {

    private Boolean cppStatus;
    private static final String TAG = "ChangeProfilePicControl";
    private static final int IMAGE_PICK_REQUEST = 100;

    private final ProfileFragment fragment;

    public ChangeProfilePicControl(ProfileFragment fragment) {
        this.fragment = fragment;
    }

    public void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        fragment.startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void uploadImage(Uri imageUri) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + FirebaseAuth.getInstance().getCurrentUser().getUid());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        updateProfilePicture(imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle unsuccessful upload
                    Log.e(TAG, "Failed to upload image to Firebase Storage: " + e.getMessage());
                });
    }

    private void updateProfilePicture(String imageUrl) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("profileImageUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    fragment.updateUserProfilePicture(imageUrl);
                    cppStatus = true;
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Log.e(TAG, "Failed to update profile image URL: " + e.getMessage());
                });
    }
}
