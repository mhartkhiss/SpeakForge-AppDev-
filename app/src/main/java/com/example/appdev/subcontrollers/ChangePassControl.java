package com.example.appdev.subcontrollers;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.example.appdev.R;
import com.example.appdev.utils.CustomNotification;
import com.example.appdev.fragments.ProfileFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePassControl implements View.OnClickListener {

    private Context context;
    private EditText editTextOldPassword, editTextNewPassword, editTextConfirmPassword;
    private CardView cardViewChangePassword, cardViewProfile;
    private ProfileFragment profileFragment;

    public ChangePassControl(Context context, EditText editTextOldPassword, EditText editTextNewPassword,
                           EditText editTextConfirmPassword, CardView cardViewChangePassword, CardView cardViewProfile, 
                           ProfileFragment profileFragment) {
        this.context = context;
        this.editTextOldPassword = editTextOldPassword;
        this.editTextNewPassword = editTextNewPassword;
        this.editTextConfirmPassword = editTextConfirmPassword;
        this.cardViewChangePassword = cardViewChangePassword;
        this.cardViewProfile = cardViewProfile;
        this.profileFragment = profileFragment;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnChangePassword2) {
            validatePasswordFields();
        }
    }

    private void validatePasswordFields() {
        String oldPassword = editTextOldPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            CustomNotification.showNotification((android.app.Activity) context, "Please fill in all fields", false);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            CustomNotification.showNotification((android.app.Activity) context, 
                "New password and confirm password do not match", false);
            return;
        }

        changePassword(oldPassword, newPassword);
    }

    private void changePassword(String oldPassword, String newPassword) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                CustomNotification.showNotification((android.app.Activity) context, 
                                    "Password changed successfully", true);
                                editTextOldPassword.setText("");
                                editTextNewPassword.setText("");
                                editTextConfirmPassword.setText("");
                            })
                            .addOnFailureListener(e -> 
                                CustomNotification.showNotification((android.app.Activity) context, 
                                    "Failed to change password: " + e.getMessage(), false));
                })
                .addOnFailureListener(e -> 
                    CustomNotification.showNotification((android.app.Activity) context, 
                        "Failed to reauthenticate: " + e.getMessage(), false));
    }
}

