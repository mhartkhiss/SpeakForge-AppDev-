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

        // Load and start the animation
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_scale);
        splashLogo.startAnimation(scaleAnimation);

        // Create a handler to start the main activity after the splash duration
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, WelcomeScreen.class);
            startActivity(intent);
            finish();
            // Use a fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION);
    }

    @Override
    public void onBackPressed() {
        // Disable back button during splash screen
    }
} 