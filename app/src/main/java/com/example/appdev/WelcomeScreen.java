package com.example.appdev;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appdev.utils.CustomDialog;
import com.example.appdev.utils.CustomNotification;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WelcomeScreen extends AppCompatActivity {

    private Button btnWLogin, btnWSkip;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private android.app.ProgressDialog progressDialog;
    private LinearLayout textContent, buttonContainer, dotsIndicator;
    private ImageView welcomeImage;
    private TextSwitcher textSwitcher;
    private View[] dots;
    private int currentTextIndex = 0;
    private final String[] featureTexts = {
        "Powered by Claude, DeepSeek, Gemini",
        "Speech Recognition and Translate text to your preferred language",
        "Chat feature that will automatically translate your message to contact's language",
        "Regenerate translation in chat if the translate text is not accurate"
    };
    private Handler autoSwitchHandler = new Handler();
    private final int AUTO_SWITCH_DELAY = 4000; // 4 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable content transitions
        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        getWindow().setSharedElementEnterTransition(TransitionInflater.from(this)
                .inflateTransition(R.transition.change_bounds_fade));
        
        setContentView(R.layout.activity_welcome_screen);

        // Find views
        textContent = findViewById(R.id.textContent);
        buttonContainer = findViewById(R.id.buttonContainer);
        welcomeImage = findViewById(R.id.imageView4);
        textSwitcher = findViewById(R.id.textSwitcher);
        dotsIndicator = findViewById(R.id.dotsIndicator);
        
        // Initialize dots
        dots = new View[4];
        dots[0] = findViewById(R.id.dot1);
        dots[1] = findViewById(R.id.dot2);
        dots[2] = findViewById(R.id.dot3);
        dots[3] = findViewById(R.id.dot4);
        
        // Set up dot click listeners
        for (int i = 0; i < dots.length; i++) {
            final int position = i;
            dots[i].setOnClickListener(v -> {
                stopAutoSwitching();
                switchToPosition(position);
                startAutoSwitching();
            });
        }
        
        // Set up TextSwitcher
        setupTextSwitcher();
        
        // Initially hide the text and buttons
        textContent.setAlpha(0f);
        buttonContainer.setAlpha(0f);
        
        // First check if user is already logged in before animating buttons
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, hide the buttons completely
            buttonContainer.setVisibility(View.GONE);
        }
        
        // Postpone the shared element transition until our animations are ready
        supportPostponeEnterTransition();
        
        // Schedule animations to start after the shared element transition
        welcomeImage.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                welcomeImage.getViewTreeObserver().removeOnPreDrawListener(this);
                
                // Start the shared element transition
                supportStartPostponedEnterTransition();
                
                // After the transition completes, start our content animations
                new Handler().postDelayed(() -> {
                    // Animate text and buttons with a sequence
                    textContent.animate()
                        .alpha(1f)
                        .setDuration(500)
                        .setInterpolator(new DecelerateInterpolator());
                    
                    // Only animate buttons if they're visible (user not logged in)
                    if (buttonContainer.getVisibility() == View.VISIBLE) {
                        buttonContainer.animate()
                            .alpha(1f)
                            .setDuration(500)
                            .setStartDelay(200)
                            .setInterpolator(new DecelerateInterpolator());
                    }
                    
                    // Finish the SplashActivity to prevent it from showing when pressing back
                    if (SplashActivity.getInstance() != null) {
                        SplashActivity.getInstance().finish();
                    }
                }, 300);
                
                return true;
            }
        });

        btnWLogin = findViewById(R.id.btnWLogin);
        btnWSkip = findViewById(R.id.btnWSkip);

        progressDialog = new android.app.ProgressDialog(WelcomeScreen.this);

        // Setup auth listener
        setupAuthListener();

        btnWLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add a subtle button animation
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100);
                        
                        // Start the login activity
                        startActivity(new Intent(WelcomeScreen.this, LoginActivity.class));
                    });
            }
        });

        btnWSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add a subtle button animation
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100);
                        
                        // Set guest user variables without authentication
                        Variables.userUID = "guest";
                        Variables.userEmail = "guest@speakforge.app";
                        Variables.userAccountType = "guest";
                        // Set default language and translator for guest
                        Variables.userLanguage = "English";
                        Variables.userTranslator = "claude";
                        
                        // Save guest user state in SharedPreferences
                        SharedPreferences prefs = getSharedPreferences(Variables.PREFS_NAME, MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(Variables.PREF_IS_GUEST_USER, true);
                        editor.apply();
                        
                        // Go directly to main activity, skipping language setup
                        Intent intent = new Intent(WelcomeScreen.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
            }
        });
    }

    private void setupAuthListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // Hide buttons when user is already logged in
                    buttonContainer.setVisibility(View.GONE);
                    
                    if (!"guest".equals(Variables.userUID)) {
                        CustomNotification.showNotification(WelcomeScreen.this, "Welcome back " + user.getEmail(), true);
                    }
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                                            // Update last login date for auto-login using ISO format for consistency with admin panel
                            String currentTimestamp = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(new java.util.Date());
                            userRef.child("lastLoginDate").setValue(currentTimestamp);
                                
                                String language = dataSnapshot.child("language").getValue(String.class);
                                if (language != null) {
                                    Intent intent = new Intent(WelcomeScreen.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    startActivity(new Intent(WelcomeScreen.this, LanguageSetupActivity.class));
                                }
                                if (!dataSnapshot.hasChild("translator")) {
                                    userRef.child("translator").setValue("google");
                                }
                            } else {
                                // Handle case when user data doesn't exist in the database
                                startActivity(new Intent(WelcomeScreen.this, LanguageSetupActivity.class));
                            }
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            CustomDialog.showDialog(WelcomeScreen.this, "Database Error", databaseError.getMessage());
                            // Still finish the activity to prevent being stuck
                            finish();
                        }
                    });
                } else {
                    // Show buttons only when no user is logged in
                    buttonContainer.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private void setupTextSwitcher() {
        // Create text views for the TextSwitcher
        textSwitcher.setFactory(() -> {
            TextView textView = new TextView(WelcomeScreen.this);
            textView.setLayoutParams(new TextSwitcher.LayoutParams(
                    TextSwitcher.LayoutParams.MATCH_PARENT,
                    TextSwitcher.LayoutParams.WRAP_CONTENT));
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            textView.setLineSpacing(4, 1);
            textView.setTypeface(ResourcesCompat.getFont(WelcomeScreen.this, R.font.poppins));
            return textView;
        });
        
        // Set initial text
        textSwitcher.setText(featureTexts[currentTextIndex]);
        updateDots(currentTextIndex);
        
        // Set click listener to manually change text
        textSwitcher.setOnClickListener(v -> {
            // Stop auto-switching
            stopAutoSwitching();
            
            // Move to next text
            currentTextIndex = (currentTextIndex + 1) % featureTexts.length;
            textSwitcher.setText(featureTexts[currentTextIndex]);
            updateDots(currentTextIndex);
            
            // Restart auto-switching
            startAutoSwitching();
        });
        
        // Start auto-switching
        startAutoSwitching();
        
        // Log initial state
        Log.d("WelcomeScreen", "TextSwitcher setup complete. Initial text: " + featureTexts[currentTextIndex]);
    }
    
    private void switchToPosition(int position) {
        Log.d("WelcomeScreen", "Switching to position: " + position);
        currentTextIndex = position;
        textSwitcher.setText(featureTexts[currentTextIndex]);
        updateDots(currentTextIndex);
    }
    
    private void updateDots(int selectedPosition) {
        Log.d("WelcomeScreen", "Updating dots to position: " + selectedPosition);
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackground(getDrawable(i == selectedPosition ? 
                    R.drawable.dot_selected : R.drawable.dot_unselected));
        }
    }
    
    private void startAutoSwitching() {
        // Clear any existing callbacks to prevent multiple runnables
        stopAutoSwitching();
        // Post new delayed runnable
        Log.d("WelcomeScreen", "Starting auto-switching from index: " + currentTextIndex);
        autoSwitchHandler.postDelayed(autoSwitchRunnable, AUTO_SWITCH_DELAY);
    }
    
    private void stopAutoSwitching() {
        Log.d("WelcomeScreen", "Stopping auto-switching");
        autoSwitchHandler.removeCallbacks(autoSwitchRunnable);
    }
    
    private final Runnable autoSwitchRunnable = new Runnable() {
        @Override
        public void run() {
            // Move to next text
            int nextIndex = (currentTextIndex + 1) % featureTexts.length;
            Log.d("WelcomeScreen", "Auto-switching from " + currentTextIndex + " to " + nextIndex);
            switchToPosition(nextIndex);
            
            // Schedule next switch
            autoSwitchHandler.postDelayed(this, AUTO_SWITCH_DELAY);
        }
    };
    
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

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoSwitching();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        startAutoSwitching();
    }
}
