// UserLanguageControl.java
package com.example.appdev.controller;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.appdev.fragments.ProfileFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserLanguageControl {

    private boolean status;
    private static final String TAG = "UserLanguageControl";
    private final ProfileFragment fragment;

    public UserLanguageControl(ProfileFragment fragment) {
        this.fragment = fragment;
    }

    public void updateUserLanguage(String language, Button btnChangeLanguage) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            userRef.child("language").setValue(language)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Language updated successfully
                            btnChangeLanguage.setText("Language: " + language);
                            Toast.makeText(fragment.getActivity(), "Language updated successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Failed to update language
                            Toast.makeText(fragment.getActivity(), "Failed to update language", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to update language: " + e.getMessage());
                        }
                    });
        }
        fragment.getCardViewLanguage().setVisibility(View.GONE);
        fragment.getCardViewProfile().setVisibility(View.VISIBLE);
    }
}
