package com.example.appdev.controller;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.appdev.LanguageSetupActivity;
import com.example.appdev.LoginActivity;
import com.example.appdev.MainActivity;
import com.example.appdev.R;
import com.example.appdev.SignUpActivity;
import com.example.appdev.classes.Variables;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginControl {
    private Boolean loginStatus = false;
    private LoginActivity loginActivity;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    public LoginControl(LoginActivity loginActivity) {
        this.loginActivity = loginActivity;
        initializeFirebaseAuth();
    }

    private void initializeFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                loginStatus = true;
                if (!Variables.guestUser.equals(user.getEmail())) {
                    Toast.makeText(loginActivity, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
                }
                // Check if the user has the sourceLanguage field in the users database
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.hasChild("language")) {
                            // User has the sourceLanguage field, start MainActivity
                            loginActivity.startActivity(new Intent(loginActivity, MainActivity.class));
                        } else {
                            // User does not have the sourceLanguage field, start LanguageSetupActivity
                            loginActivity.startActivity(new Intent(loginActivity, LanguageSetupActivity.class));
                        }
                        loginActivity.finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle database error
                        Toast.makeText(loginActivity, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    public void loginUser(String email, String password) {

        // Check if email is empty or invalid
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(loginActivity, "Invalid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if password is empty
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(loginActivity, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sign in the user with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(loginActivity, task -> {
                    if (!task.isSuccessful()) {
                        // If sign in fails,
                        loginStatus = false;
                        Toast.makeText(loginActivity, "Authentication failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                    }
                });


    }
    public void onStart() {
        mAuth.addAuthStateListener(mAuthListener);
    }

    public void onStop() {
        if (mAuthListener != null) {
            loginStatus = false;
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}