package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2800; // 2.8 seconds
    private ImageView splashLogo;
    private TypeWriter splashCatchphrase;
    private final String catchphraseText = "Forging connections with AI-Powered Translation";
    private final int FINAL_LOGO_SIZE_DP = 280; // Target size matching welcome screen
    
    // Static instance for reference
    private static SplashActivity instance;
    
    // Static getter for the instance
    public static SplashActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the static instance
        instance = this;
        
        // Enable content transitions
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setExitTransition(null); // Disable exit transition to prevent slide down
        
        setContentView(R.layout.activity_splash_screen);

        // Initialize views
        splashLogo = findViewById(R.id.splashLogo);
        splashCatchphrase = findViewById(R.id.splashCatchphrase);

        // Start animations sequence
        startAnimations();

        // Create a handler to start the main activity after the splash duration
        new Handler().postDelayed(this::startWelcomeScreenWithTransition, SPLASH_DURATION);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear the static instance when destroyed
        if (instance == this) {
            instance = null;
        }
    }

    private void startAnimations() {
        // Logo fade in animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in);
        splashLogo.startAnimation(fadeIn);
        
        // Start typing animation after logo appears
        new Handler().postDelayed(() -> {
            // Start typing animation
            splashCatchphrase.setCharacterDelay(40); // 40ms between characters
            splashCatchphrase.animateText(catchphraseText);
        }, 600);
        
        // Start logo size reduction animation near the end
        new Handler().postDelayed(this::animateLogoSizeReduction, SPLASH_DURATION - 800);
    }
    
    private void animateLogoSizeReduction() {
        // Convert dp to pixels for animation
        final float density = getResources().getDisplayMetrics().density;
        final int startSize = Math.round(350 * density);
        final int endSize = Math.round(FINAL_LOGO_SIZE_DP * density);
        
        // Create value animator for width and height
        ValueAnimator sizeAnimator = ValueAnimator.ofInt(startSize, endSize);
        sizeAnimator.setDuration(700); // Duration of size animation
        sizeAnimator.setInterpolator(new DecelerateInterpolator());
        
        sizeAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams layoutParams = splashLogo.getLayoutParams();
            layoutParams.width = animatedValue;
            layoutParams.height = animatedValue;
            splashLogo.setLayoutParams(layoutParams);
        });
        
        sizeAnimator.start();
    }

    private void startWelcomeScreenWithTransition() {
        // Check if user is already in guest mode
        SharedPreferences prefs = getSharedPreferences(Variables.PREFS_NAME, MODE_PRIVATE);
        boolean isGuestUser = prefs.getBoolean(Variables.PREF_IS_GUEST_USER, false);
        
        Intent intent;
        
        if (isGuestUser) {
            // If guest user, set guest variables and go directly to MainActivity
            Variables.userUID = "guest";
            Variables.userEmail = "guest@speakforge.app";
            Variables.userAccountType = "guest";
            Variables.userLanguage = "English";
            Variables.userTranslator = "claude";
            
            intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish the splash activity
        } else {
            // Regular flow - go to WelcomeScreen with transition
            intent = new Intent(SplashActivity.this, WelcomeScreen.class);
            
            // Create the transition animation
            Pair<View, String> logoPair = Pair.create((View)splashLogo, "logoTransition");
            
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this, logoPair);
            
            // Start the activity with the transition
            startActivity(intent, options.toBundle());
            
            // Don't call finish() immediately to allow the shared element transition to complete
            // Instead, let the WelcomeScreen handle finishing this activity
            // This prevents the slide down animation
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button during splash screen
    }
}