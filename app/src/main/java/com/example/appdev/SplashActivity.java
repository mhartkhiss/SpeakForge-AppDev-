package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Initialize the logo ImageView
        ImageView splashLogo = findViewById(R.id.splashLogo);

        // Initial fade in animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in);
        splashLogo.startAnimation(fadeIn);

        // Create a handler to start the main activity after the splash duration
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, WelcomeScreen.class);
            startActivity(intent);
            finish();
            // Remove default transition animation
            overridePendingTransition(0, 0);
        }, SPLASH_DURATION);
    }

    @Override
    public void onBackPressed() {
        // Disable back button during splash screen
    }
} 