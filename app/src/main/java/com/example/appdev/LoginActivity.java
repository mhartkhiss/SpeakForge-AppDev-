package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.appdev.controller.LoginControl;

public class LoginActivity extends AppCompatActivity {

    private LoginControl loginControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        View loadingView = LayoutInflater.from(this).inflate(R.layout.loading, null);
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(loadingView);

        loginControl = new LoginControl(this);
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