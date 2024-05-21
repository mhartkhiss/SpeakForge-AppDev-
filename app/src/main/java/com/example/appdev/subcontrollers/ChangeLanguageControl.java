package com.example.appdev.subcontrollers;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.appdev.Variables;
import com.example.appdev.fragments.ProfileFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class ChangeLanguageControl {

    private final ProfileFragment profileFragment;

    public ChangeLanguageControl(ProfileFragment fragment) {
        this.profileFragment = fragment;
    }

    public void updateUserLanguage(String selectedLanguage, Button btnChangeLanguage) {

        if (Variables.currentUser != null) {
            Variables.userRef.child("language").setValue(selectedLanguage)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            btnChangeLanguage.setText("Language: " + selectedLanguage);
                            Toast.makeText(profileFragment.getActivity(), "Language updated successfully", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(profileFragment.getActivity(), "Failed to update language", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        profileFragment.getLayoutLanguageSelection().setVisibility(View.GONE);
        profileFragment.getLayoutProfile().setVisibility(View.VISIBLE);
    }
}
