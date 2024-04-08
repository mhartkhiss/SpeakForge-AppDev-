package com.example.appdev.fragments;

import static android.content.ContentValues.TAG;

import static com.example.appdev.MainActivity.currentUser;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.appdev.LoginActivity;
import com.example.appdev.R;
import com.example.appdev.classes.Variables;
import com.example.appdev.models.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
    private CardView cardViewEditDetails, cardViewProfile, cardViewLanguage;
    private Button btnSaveChanges, btnChangeLanguage, btnChangePassword, btnEnglish, btnTagalog, btnBisaya;

    private EditText editTextUsername;
    private static final int IMAGE_PICK_REQUEST = 100;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize TextViews
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        imageViewUserPicture = view.findViewById(R.id.imageViewUserPicture);
        cardViewEditDetails = view.findViewById(R.id.cardViewEditDetails);
        cardViewProfile = view.findViewById(R.id.cardViewProfile);
        cardViewLanguage = view.findViewById(R.id.cardViewLanguage);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnChangeLanguage = view.findViewById(R.id.btnChangeLanguage);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnEnglish = view.findViewById(R.id.btnEnglish);
        btnTagalog = view.findViewById(R.id.btnTagalog);
        btnBisaya = view.findViewById(R.id.btnBisaya);
        editTextUsername = view.findViewById(R.id.editTextUsername);

        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (Variables.guestUser.equals(userEmail)) {
            view.setVisibility(View.GONE);
        }



        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logout());

        setUserDetails();


        imageViewUserPicture.setOnClickListener(this::selectImage);

        textViewUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardViewEditDetails.setVisibility(View.VISIBLE);
                cardViewProfile.setVisibility(View.GONE);
            }
        });

        btnChangeLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardViewLanguage.setVisibility(View.VISIBLE);
                cardViewProfile.setVisibility(View.GONE);
            }
        });

        btnEnglish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSourceLanguage("English");
            }
        });

        btnTagalog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSourceLanguage("Tagalog");
            }
        });

        btnBisaya.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSourceLanguage("Bisaya");
            }
        });

        btnSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the new username
                String newUsername = editTextUsername.getText().toString().trim();

                // Check if the new username is not empty
                if (!newUsername.isEmpty()) {
                    // Get the current user's UID
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String uid = currentUser.getUid();

                        // Update the username in the Firebase Realtime Database
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                        userRef.child("username").setValue(newUsername)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        textViewUsername.setText(newUsername);
                                        Toast.makeText(getActivity(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Failed to update username
                                        Toast.makeText(getActivity(), "Failed to update username", Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, "Failed to update username: " + e.getMessage());
                                    }
                                });
                    }
                } else {
                    // Show error message if username is empty
                    Toast.makeText(getActivity(), "Please enter a username", Toast.LENGTH_SHORT).show();
                }

                // Hide the edit cardView and show the original cardView
                cardViewEditDetails.setVisibility(View.GONE);
                cardViewProfile.setVisibility(View.VISIBLE);
            }
        });


    }

    private void updateSourceLanguage(String language) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            userRef.child("sourceLanguage").setValue(language)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Language updated successfully
                            Toast.makeText(getActivity(), "Language updated successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to update language
                            Toast.makeText(getActivity(), "Failed to update language", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to update language: " + e.getMessage());
                        }
                    });
        }
        cardViewLanguage.setVisibility(View.GONE);
        cardViewProfile.setVisibility(View.VISIBLE);
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
                            editTextUsername.setText(username);
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
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("profile_pictures/" + FirebaseAuth.getInstance().getCurrentUser().getUid());

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
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
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("profileImageUrl").setValue(imageUrl)
                .addOnSuccessListener(aVoid -> {
                    Glide.with(this).load(imageUrl).into(imageViewUserPicture);
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    Log.e(TAG, "Failed to update profile image URL: " + e.getMessage());
                });
    }

}
