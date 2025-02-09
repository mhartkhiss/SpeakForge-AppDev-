package com.example.appdev.subcontrollers;

import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.appdev.R;
import com.example.appdev.adapters.LanguageAdapter;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.utils.CustomNotification;
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
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());

            userRef.child("language").setValue(selectedLanguage)
                    .addOnSuccessListener(aVoid -> 
                        CustomNotification.showNotification(profileFragment.requireActivity(), 
                            "Language updated successfully", true))
                    .addOnFailureListener(e -> 
                        CustomNotification.showNotification(profileFragment.requireActivity(), 
                            "Failed to update language", false));
        }
    }
}
