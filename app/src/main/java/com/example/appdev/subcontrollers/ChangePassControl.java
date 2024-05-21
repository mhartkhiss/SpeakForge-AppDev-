package com.example.appdev.subcontrollers;

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
        String oldPassword = editTextOldPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(context, "New password and confirm password do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        changePassword(oldPassword, newPassword);
    }

    private void changePassword(String oldPassword, String newPassword) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        currentUser.updatePassword(newPassword)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show();
                                        editTextOldPassword.setText("");
                                        editTextNewPassword.setText("");
                                        editTextConfirmPassword.setText("");
                                        cardViewChangePassword.setVisibility(View.GONE);
                                        cardViewProfile.setVisibility(View.VISIBLE);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(context, "Failed to change password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Failed to reauthenticate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

