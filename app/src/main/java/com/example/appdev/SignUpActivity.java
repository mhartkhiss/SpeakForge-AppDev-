package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ImageView;

import com.example.appdev.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.appdev.utils.CustomDialog;

public class SignUpActivity extends BaseAuthActivity implements View.OnClickListener {

    private Boolean isSignUpValid;
    private ProgressDialog progressDialog;
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        ImageView logoImage = findViewById(R.id.speakForgeLogo);
        setupKeyboardVisibilityListener(logoImage);
        
        // Initialize views
        LinearLayout formContainer = findViewById(R.id.signupFormContainer);
        TextView txtLogin = findViewById(R.id.txtLogin);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);

        // Initially hide views
        logoImage.setVisibility(View.INVISIBLE);
        formContainer.setVisibility(View.INVISIBLE);

        // Load animations
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_fade_in_logo);
        Animation formAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);

        // Show and animate views
        logoImage.setVisibility(View.VISIBLE);
        formContainer.setVisibility(View.VISIBLE);
        
        logoImage.startAnimation(logoAnimation);
        formContainer.startAnimation(formAnimation);

        progressDialog = new ProgressDialog(this);

        txtLogin.setOnClickListener(this);
        btnSignUp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnSignUp) {
            signUpUser();
        } else if (v.getId() == R.id.txtLogin) {
            goToLoginActivity();
        }
    }

    private void signUpUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        isSignUpValid = validateSignUpFields(email, password, confirmPassword);

        if (isSignUpValid) {
            progressDialog.show();
            progressDialog.setText("Signing up...");
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                String username = email;
                                String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(userId)
                                        .setValue(new User(userId, username, email, "none", "free", null, timestamp, timestamp, "google"))
                                        .addOnCompleteListener(databaseTask -> {
                                            if (databaseTask.isSuccessful()) {
                                                CustomDialog.showDialog(this, "Success", "Sign up successful", 
                                                    (dialog, which) -> {
                                                        FirebaseAuth.getInstance().signOut();
                                                        goToLoginActivity();
                                                    });
                                            } else {
                                                CustomDialog.showDialog(this, "Error", "Failed to store user data in database");
                                            }
                                        });
                            }
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                CustomDialog.showDialog(this, "Email Error", "Email address is already in use");
                            } else {
                                CustomDialog.showDialog(this, "Sign Up Failed", "Please try again later");
                            }
                        }
                    });
        }
    }

    private boolean validateSignUpFields(String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            CustomDialog.showDialog(this, "Invalid Email", "Please enter a valid email address");
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            CustomDialog.showDialog(this, "Invalid Password", "Password must be at least 6 characters long");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            CustomDialog.showDialog(this, "Password Mismatch", "Passwords do not match");
            return false;
        }

        return true;
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goToLoginActivity();
    }

    @Override
    public void finish() {
        super.finish();
        if (!isFinishing()) {
            overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
        }
    }

}