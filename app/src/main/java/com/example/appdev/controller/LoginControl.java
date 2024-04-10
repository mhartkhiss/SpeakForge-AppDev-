package com.example.appdev.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.appdev.MainActivity;
import com.example.appdev.R;
import com.example.appdev.SignUpActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginControl implements View.OnClickListener {

    private Context context;
    private FirebaseAuth mAuth;

    public LoginControl(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnLogin) {
            loginUser();
        } else if (v.getId() == R.id.txtSignUp) {
            goToSignUpActivity();
        }
    }

    public void setListeners() {
        Button btnLogin = ((Activity) context).findViewById(R.id.btnLogin);
        TextView txtSignUp = ((Activity) context).findViewById(R.id.txtSignUp);

        txtSignUp.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
    }

    private void loginUser() {
        EditText emailEditText = ((Activity) context).findViewById(R.id.email);
        EditText passwordEditText = ((Activity) context).findViewById(R.id.password);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check if email is empty or invalid
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            hideProgressBar();
            Toast.makeText(context, "Invalid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if password is empty
        if (TextUtils.isEmpty(password)) {
            hideProgressBar();
            Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sign in the user with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener((Activity) context, task -> {
                    if (!task.isSuccessful()) {
                        // If sign in fails, display a message to the user
                        hideProgressBar();
                        Toast.makeText(context, "Authentication failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void goToSignUpActivity() {
        Intent intent = new Intent(context, SignUpActivity.class);
        context.startActivity(intent);
    }

    private void hideProgressBar() {
        ProgressBar progressBar = ((Activity) context).findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
}
