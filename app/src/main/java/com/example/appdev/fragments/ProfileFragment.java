package com.example.appdev.fragments;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.appdev.LoginActivity;
import com.example.appdev.R;
import com.example.appdev.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

public class ProfileFragment extends Fragment {

    private TextView textViewUsername;
    private TextView textViewEmail;
    private ImageView imageViewUserPicture;
    private static final int IMAGE_PICK_REQUEST = 100;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize TextViews
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logout());

        // Set user details
        setUserDetails();

        imageViewUserPicture = view.findViewById(R.id.imageViewUserPicture);
        imageViewUserPicture.setOnClickListener(this::selectImage);
    }

    private void setUserDetails() {
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Set email
            String email = currentUser.getEmail();
            if (email != null && !email.isEmpty()) {
                textViewEmail.setText(email);
            }

            // Retrieve username and profile picture URL from the database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                        if (username != null && !username.isEmpty()) {
                            textViewUsername.setText(username);
                        }
                        if (profileImageUrl != null && !profileImageUrl.equals("none")) {
                            // Load and display profile picture using Glide or similar library
                            Glide.with(requireContext()).load(profileImageUrl).into(imageViewUserPicture);
                        } else {
                            // Set a default image if no profile picture is available
                            imageViewUserPicture.setImageResource(R.drawable.default_userpic);
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }



    private void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(requireActivity(), LoginActivity.class));
        requireActivity().finish();
    }

    public void selectImage(View view) {
        // Launch intent to pick an image from gallery
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void uploadImage(Uri imageUri) {
        // Create a reference to the Firebase Storage location where you want to store the image
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + FirebaseAuth.getInstance().getCurrentUser().getUid());

        // Upload the image to Firebase Storage
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Image uploaded successfully, now get the download URL
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Save the download URL to Firebase Realtime Database or Firestore
                        String imageUrl = uri.toString();
                        updateUserProfilePicture(imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle unsuccessful upload
                    Log.e(TAG, "Failed to upload image to Firebase Storage: " + e.getMessage());
                });
    }

    private void updateUserProfilePicture(String imageUrl) {
        // Update the user's profile in Firebase Realtime Database or Firestore with the new image URL
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("profileImageUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    // Profile image URL updated successfully
                    // Now you can load and display the updated profile picture using Glide or similar library
                    Glide.with(this).load(imageUrl).into(imageViewUserPicture);
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Log.e(TAG, "Failed to update profile image URL: " + e.getMessage());
                });
    }

}
