package com.example.appdev.subcontrollers;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.appdev.Variables;
import com.example.appdev.fragments.ProfileFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChangeLanguageControl {

    private final ProfileFragment profileFragment;

    public ChangeLanguageControl(ProfileFragment fragment) {
        this.profileFragment = fragment;
    }

    public void updateUserLanguage(String selectedLanguage) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        if ( currentUser != null) {
            userRef.child("language").setValue(selectedLanguage)
                    .addOnSuccessListener(aVoid -> Toast.makeText(profileFragment.getActivity(), "Language updated successfully", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(profileFragment.getActivity(), "Failed to update language", Toast.LENGTH_SHORT).show());
        }
        profileFragment.getLayoutLanguageSelection().setVisibility(View.GONE);
        profileFragment.getLayoutProfile().setVisibility(View.VISIBLE);
    }
}
