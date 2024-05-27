package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;
    private Button resetPasswordButton;
    private TextView emailSentLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        progressDialog = new ProgressDialog(ForgotPasswordActivity.this);
        resetPasswordButton = findViewById(R.id.btnResetPassword);
        emailSentLabel = findViewById(R.id.txtEmailSent);
        setListeners();
    }

    private void setListeners() {
        resetPasswordButton.setOnClickListener(v -> resetPassword());
        TextView backToLogin = findViewById(R.id.txtBackToLogin);
        backToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void resetPassword() {
        EditText emailEditText = findViewById(R.id.email);
        String emailAddress = emailEditText.getText().toString().trim();

        if (emailAddress.isEmpty()) {
            Toast.makeText(ForgotPasswordActivity.this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        progressDialog.setMessage("Please wait...");

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
        databaseReference.orderByChild("email").equalTo(emailAddress)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            sendResetEmail(emailAddress);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(ForgotPasswordActivity.this, "Email not found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressDialog.dismiss();
                        Toast.makeText(ForgotPasswordActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendResetEmail(String emailAddress) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Email sent.", Toast.LENGTH_SHORT).show();
                        emailSentLabel.setVisibility(View.VISIBLE);
                        startTimer();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset email!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startTimer() {
        resetPasswordButton.setEnabled(false);
        new CountDownTimer(60000, 1000) { // 1 minute timer
            public void onTick(long millisUntilFinished) {
                resetPasswordButton.setText("Try again in " + millisUntilFinished / 1000 + " seconds");
            }

            public void onFinish() {
                emailSentLabel.setVisibility(View.GONE);
                resetPasswordButton.setText("RESET PASSWORD");
                resetPasswordButton.setEnabled(true);
            }
        }.start();
    }
}
