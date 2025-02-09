package com.example.appdev.subcontrollers;

import android.text.TextUtils;
import android.widget.EditText;
import androidx.cardview.widget.CardView;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChangeUsernameControl {
    private final ProfileFragment profileFragment;
    private final EditText editTextUsername;
    private final CardView layoutChangeUsername;
    private final CardView layoutProfile;

    public ChangeUsernameControl(ProfileFragment fragment, EditText editTextUsername,
                               CardView layoutChangeUsername, CardView layoutProfile) {
        this.profileFragment = fragment;
        this.editTextUsername = editTextUsername;
        this.layoutChangeUsername = layoutChangeUsername;
        this.layoutProfile = layoutProfile;
    }

    public void updateUsername() {
        String newUsername = editTextUsername.getText().toString().trim();
        if (newUsername.isEmpty()) {
            editTextUsername.setError("Username cannot be empty");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("username").setValue(newUsername)
                .addOnSuccessListener(aVoid -> {
                    CustomNotification.showNotification(profileFragment.requireActivity(), 
                        "Username updated successfully", true);
                })
                .addOnFailureListener(e -> {
                    CustomNotification.showNotification(profileFragment.requireActivity(), 
                        "Failed to update username", false);
                });
    }
} 