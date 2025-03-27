package com.example.appdev.subcontrollers;

import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import androidx.cardview.widget.CardView;
import com.example.appdev.R;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

    public void setupUsernameDialog(View view, BottomSheetDialog dialog, String currentUsername) {
        EditText usernameInput = view.findViewById(R.id.editTextUsername);
        Button btnSave = view.findViewById(R.id.btnSaveChanges);
        usernameInput.setText(currentUsername);

        btnSave.setOnClickListener(v -> {
            String newUsername = usernameInput.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                updateUsername(newUsername, dialog);
            }
        });
    }

    public void updateUsername(String newUsername, BottomSheetDialog dialog) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("username").setValue(newUsername)
                .addOnSuccessListener(aVoid -> {
                    CustomNotification.showNotification(profileFragment.requireActivity(), 
                        "Username updated successfully", true);
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    CustomNotification.showNotification(profileFragment.requireActivity(), 
                        "Failed to update username", false);
                });
    }
} 