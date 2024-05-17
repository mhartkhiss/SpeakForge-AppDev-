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

import com.example.appdev.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private Boolean isSignUpValid;
    private ProgressDialog progressDialog;
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        TextView txtLogin = findViewById(R.id.txtLogin);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        confirmPasswordEditText = findViewById(R.id.confirmPassword);

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
                                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                FirebaseDatabase.getInstance().getReference("users")
                                        .child(userId)
                                        .setValue(new User(userId, username, email, "none", "user", null, timestamp, timestamp))
                                        .addOnCompleteListener(databaseTask -> {
                                            if (databaseTask.isSuccessful()) {
                                                Toast.makeText(this, "Sign up successful", Toast.LENGTH_SHORT).show();
                                                FirebaseAuth.getInstance().signOut();
                                                goToLoginActivity();
                                            } else {
                                                Toast.makeText(this, "Failed to store user data in database", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                Toast.makeText(this, "Email address is already in use", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Sign up failed. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private boolean validateSignUpFields(String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email address", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void goToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}