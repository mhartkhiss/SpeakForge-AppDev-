package com.example.appdev.subcontrollers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.core.content.ContextCompat;
import com.example.appdev.R;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.translators.TranslatorType;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import androidx.fragment.app.Fragment;
import android.app.Dialog;
import androidx.annotation.NonNull;

public class ChangeTranslatorControl {
    private final Fragment fragment;
    private final Context context;

    public ChangeTranslatorControl(Fragment fragment) {
        this.fragment = fragment;
        this.context = fragment.requireContext();
    }

    // Method for BottomSheetDialog
    public void setupTranslatorButtons(ViewGroup container, BottomSheetDialog dialog) {
        setupTranslatorButtonsInternal(container, () -> {
            if (dialog != null) dialog.dismiss();
        });
    }

    // Method for regular Dialog
    public void setupTranslatorButtons(ViewGroup container, Dialog dialog) {
        setupTranslatorButtonsInternal(container, () -> {
            if (dialog != null) dialog.dismiss();
        });
    }

    // Method for no dialog
    public void setupTranslatorButtons(ViewGroup container) {
        setupTranslatorButtonsInternal(container, null);
    }

    // Internal method to avoid code duplication
    private void setupTranslatorButtonsInternal(ViewGroup container, Runnable onDismiss) {
        for (TranslatorType type : TranslatorType.values()) {
            MaterialButton button = (MaterialButton) LayoutInflater.from(context)
                .inflate(R.layout.translator_button, container, false);
            
            button.setText(type.getDisplayName());
            button.setIcon(ContextCompat.getDrawable(context, type.getIconResourceId()));
            button.setOnClickListener(v -> {
                checkPremiumAndUpdateTranslator(type.getId(), type.getDisplayName(), onDismiss);
            });
            
            container.addView(button);
        }
    }

    private void checkPremiumAndUpdateTranslator(String translatorType, String displayName, Runnable onDismiss) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        
        userRef.child("accountType").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String accountType = snapshot.getValue(String.class);
                
                if (accountType != null && accountType.equals("premium")) {
                    // User is premium, allow translator change
                    updateTranslator(translatorType, displayName);
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                } else {
                    // User is not premium, show notification
                    CustomNotification.showNotification(fragment.requireActivity(),
                        "Premium subscription required to change translator", false);
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                CustomNotification.showNotification(fragment.requireActivity(),
                    "Failed to check account status", false);
                if (onDismiss != null) {
                    onDismiss.run();
                }
            }
        });
    }

    public void updateTranslator(String translatorType, String displayName) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("translator").setValue(translatorType)
                .addOnSuccessListener(aVoid -> {
                    CustomNotification.showNotification(fragment.requireActivity(), 
                        "Switched to " + displayName, true);
                })
                .addOnFailureListener(e -> {
                    CustomNotification.showNotification(fragment.requireActivity(), 
                        "Failed to change translator", false);
                });
    }
} 