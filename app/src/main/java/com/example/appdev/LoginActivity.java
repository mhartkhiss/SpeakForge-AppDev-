package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        View loadingView = LayoutInflater.from(this).inflate(R.layout.loading, null);
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(loadingView);
        progressDialog = new ProgressDialog(LoginActivity.this);

        initializeFirebaseAuth();
        setListeners();
    }
    private void initializeFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                progressDialog.dismiss();
                if (!Variables.guestUser.equals(user.getEmail())) {
                    Toast.makeText(LoginActivity.this, "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
                }

                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChild("apiKey")) {
                            userRef.child("apiKey").setValue("");
                        }
                        if (!dataSnapshot.hasChild("translator")) {
                            userRef.child("translator").setValue("google");
                        }
                        if (dataSnapshot.exists() && dataSnapshot.hasChild("language")) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, LanguageSetupActivity.class));
                        }
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(LoginActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    private void setListeners() {
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        TextView txtForgotPassword = findViewById(R.id.txtForgotPassword);

        txtForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
            //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnLogin.setOnClickListener(v -> {
            progressDialog.show();
            progressDialog.setText("Logging in...");
            loginUser();
        });

        btnSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
            //overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

    }
    private void loginUser() {
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            progressDialog.dismiss();
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            progressDialog.dismiss();
            Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Authentication failed. Please check your credentials.", Toast.LENGTH_SHORT).show();
                    }
                });


    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }



}