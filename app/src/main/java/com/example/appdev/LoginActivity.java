package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.appdev.controller.LoginControl;

// LoginActivity.java

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button btnLogin;
    private TextView txtSignUp;
    private ProgressBar progressBar;
    private LoginControl loginControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        loginControl = new LoginControl(this);
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        txtSignUp = findViewById(R.id.txtSignUp);
        progressBar = findViewById(R.id.progressBar);

        txtSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            loginControl.loginUser(email, password);
        });

    }

    public void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loginControl.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        loginControl.onStop();
    }
}
