package com.example.appdev.subcontrollers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.translators.TranslatorType;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChangeTranslatorControl {
    private final ProfileFragment profileFragment;
    private final Context context;

    public ChangeTranslatorControl(ProfileFragment fragment) {
        this.profileFragment = fragment;
        this.context = fragment.requireContext();
    }

    public void setupTranslatorButtons(ViewGroup container) {
        for (TranslatorType type : TranslatorType.values()) {
            MaterialButton button = (MaterialButton) LayoutInflater.from(context)
                .inflate(R.layout.translator_button, container, false);
            
            button.setText(type.getDisplayName());
            button.setIcon(ContextCompat.getDrawable(context, type.getIconResourceId()));
            button.setOnClickListener(v -> updateTranslator(type.getId(), type.getDisplayName()));
            
            container.addView(button);
        }
    }

    public void updateTranslator(String translatorType, String displayName) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("translator").setValue(translatorType)
                .addOnSuccessListener(aVoid -> {
                    CustomNotification.showNotification(profileFragment.requireActivity(), 
                        "Switched to " + displayName, true);
                })
                .addOnFailureListener(e -> {
                    CustomNotification.showNotification(profileFragment.requireActivity(), 
                        "Failed to change translator", false);
                });
    }
} 