package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.button.MaterialButton;
import com.example.appdev.utils.CustomDialog;
import com.example.appdev.utils.CustomNotification;

public class LoginActivity extends BaseAuthActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImageView logoImage = findViewById(R.id.speakForgeLogo);
        setupKeyboardVisibilityListener(logoImage);
        LinearLayout formContainer = findViewById(R.id.loginFormContainer);
        
        logoImage.setVisibility(View.INVISIBLE);
        formContainer.setVisibility(View.INVISIBLE);
        
        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_logo);
        Animation formAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_form);
        
        logoImage.setVisibility(View.VISIBLE);
        formContainer.setVisibility(View.VISIBLE);
        
        logoImage.startAnimation(logoAnimation);
        formContainer.startAnimation(formAnimation);

        logoImage.setOnClickListener(v -> {
            Animation fadeOutLogo = AnimationUtils.loadAnimation(this, R.anim.fade_out_logo);
            Animation slideDownForm = AnimationUtils.loadAnimation(this, R.anim.slide_down_form);
            
            fadeOutLogo.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // Disable click to prevent multiple triggers
                    logoImage.setEnabled(false);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Make views invisible before transition
                    logoImage.setVisibility(View.INVISIBLE);
                    formContainer.setVisibility(View.INVISIBLE);
                    
                    Intent intent = new Intent(LoginActivity.this, WelcomeScreen.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(0, 0);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            
            logoImage.startAnimation(fadeOutLogo);
            formContainer.startAnimation(slideDownForm);
        });

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
                if (!"guest".equals(Variables.userUID)) {
                    CustomNotification.showNotification(this, "Welcome " + user.getEmail(), true);
                }

                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.hasChild("translator")) {
                            userRef.child("translator").setValue("google");
                        }
                        Intent intent;
                        if (dataSnapshot.exists() && dataSnapshot.hasChild("language")) {
                            intent = new Intent(LoginActivity.this, MainActivity.class);
                        } else {
                            intent = new Intent(LoginActivity.this, LanguageSetupActivity.class);
                        }
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
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
            goToForgotPassword();
        });

        btnLogin.setOnClickListener(v -> {
            progressDialog.show();
            progressDialog.setText("Logging in...");
            loginUser();
        });

        btnSignUp.setOnClickListener(v -> {
            goToSignUp();
        });

    }

    private void loginUser() {
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            progressDialog.dismiss();
            CustomDialog.showDialog(this, "Invalid Email", "Please enter a valid email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            progressDialog.dismiss();
            CustomDialog.showDialog(this, "Empty Password", "Please enter your password");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        progressDialog.dismiss();
                        CustomDialog.showDialog(this, "Authentication Failed", "Please check your email and password");
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

    private void goToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToForgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
        finish();
    }
}