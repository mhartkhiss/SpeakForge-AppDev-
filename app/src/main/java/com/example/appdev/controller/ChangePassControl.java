package com.example.appdev.controller;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import com.example.appdev.R;
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

    public ChangePassControl(Context context, EditText editTextOldPassword, EditText editTextNewPassword,
                             EditText editTextConfirmPassword, CardView cardViewChangePassword, CardView cardViewProfile) {
        this.context = context;
        this.editTextOldPassword = editTextOldPassword;
        this.editTextNewPassword = editTextNewPassword;
        this.editTextConfirmPassword = editTextConfirmPassword;
        this.cardViewChangePassword = cardViewChangePassword;
        this.cardViewProfile = cardViewProfile;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnChangePassword2) {
            validatePasswordFields();
        }
    }

    private void validatePasswordFields() {
        // Get the text entered in the old password, new password, and confirm password fields
        String oldPassword = editTextOldPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validate that all fields are not empty
        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if new password matches confirm password
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(context, "New password and confirm password do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        changePassword(oldPassword, newPassword);
    }

    private void changePassword(String oldPassword, String newPassword) {
        // Get the current user from FirebaseAuth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Reauthenticate the user to ensure they are the legitimate owner of the account
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // If reauthentication is successful, update the password
                        currentUser.updatePassword(newPassword)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Password updated successfully
                                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                        // Clear the password fields
                                        editTextOldPassword.setText("");
                                        editTextNewPassword.setText("");
                                        editTextConfirmPassword.setText("");
                                        // Hide the change password card view and show the profile card view
                                        cardViewChangePassword.setVisibility(View.GONE);
                                        cardViewProfile.setVisibility(View.VISIBLE);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Password update failed
                                        Toast.makeText(context, "Failed to change password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Reauthentication failed
                        Toast.makeText(context, "Failed to reauthenticate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

