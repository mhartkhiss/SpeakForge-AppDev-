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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.appdev.utils.CustomDialog;

public class ForgotPasswordActivity extends BaseAuthActivity {

    private ProgressDialog progressDialog;
    private Button resetPasswordButton;
    private TextView emailSentLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views with animations
        ImageView logoImage = findViewById(R.id.speakForgeLogo);
        LinearLayout formContainer = findViewById(R.id.forgotPasswordFormContainer);
        resetPasswordButton = findViewById(R.id.btnResetPassword);
        emailSentLabel = findViewById(R.id.txtEmailSent);

        // Initially hide views
        logoImage.setVisibility(View.INVISIBLE);
        formContainer.setVisibility(View.INVISIBLE);

        // Load and start animations
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in_logo);
        Animation formAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);

        logoImage.setVisibility(View.VISIBLE);
        formContainer.setVisibility(View.VISIBLE);

        logoImage.startAnimation(logoAnimation);
        formContainer.startAnimation(formAnimation);

        progressDialog = new ProgressDialog(ForgotPasswordActivity.this);
        setListeners();
    }

    private void setListeners() {
        resetPasswordButton.setOnClickListener(v -> resetPassword());
        TextView backToLogin = findViewById(R.id.txtBackToLogin);
        backToLogin.setOnClickListener(v -> goToLoginActivity());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goToLoginActivity();
    }

    private void resetPassword() {
        EditText emailEditText = findViewById(R.id.email);
        String emailAddress = emailEditText.getText().toString().trim();

        if (emailAddress.isEmpty()) {
            CustomDialog.showDialog(this, "Empty Email", "Please enter your email");
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
                            CustomDialog.showDialog(ForgotPasswordActivity.this, "Email Not Found", "No account found with this email address");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressDialog.dismiss();
                        CustomDialog.showDialog(ForgotPasswordActivity.this, "Database Error", databaseError.getMessage());
                    }
                });
    }

    private void sendResetEmail(String emailAddress) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        CustomDialog.showDialog(this, "Email Sent", "Password reset instructions have been sent to your email");
                        emailSentLabel.setVisibility(View.VISIBLE);
                        startTimer();
                    } else {
                        CustomDialog.showDialog(this, "Email Error", "Failed to send reset email. Please try again later");
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

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
