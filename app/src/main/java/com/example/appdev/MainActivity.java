package com.example.appdev;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.appdev.fragments.BasicTranslationFragment;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.models.User;
import com.example.appdev.utils.TranslationModeManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.appdev.utils.ConnectionRequestManager;

public class MainActivity extends AppCompatActivity {

    private BasicTranslationFragment basicTranslationFragment;

    private void loadApiKeys() {
        // Existing klusterai keys loading
        DatabaseReference apiKeysRef = FirebaseDatabase.getInstance()
                .getReference("apikeys")
                .child("klusterai");

        apiKeysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Variables.klusterAiKeys.clear();
                for (DataSnapshot keySnapshot : dataSnapshot.getChildren()) {
                    String apiKey = keySnapshot.getValue(String.class);
                    if (apiKey != null) {
                        Variables.klusterAiKeys.add(apiKey);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch Kluster API keys: " + databaseError.getMessage());
            }
        });

        // Add DeepSeek keys loading
        DatabaseReference deepseekKeysRef = FirebaseDatabase.getInstance()
                .getReference("apikeys")
                .child("deepseek");

        deepseekKeysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Variables.deepseekKeys.clear();
                for (DataSnapshot keySnapshot : dataSnapshot.getChildren()) {
                    String apiKey = keySnapshot.getValue(String.class);
                    if (apiKey != null) {
                        Variables.deepseekKeys.add(apiKey);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch DeepSeek API keys: " + databaseError.getMessage());
            }
        });

        // Add Gemini keys loading
        DatabaseReference geminiKeysRef = FirebaseDatabase.getInstance()
                .getReference("apikeys")
                .child("gemini");

        geminiKeysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Variables.geminiKeys.clear();
                for (DataSnapshot keySnapshot : dataSnapshot.getChildren()) {
                    String apiKey = keySnapshot.getValue(String.class);
                    if (apiKey != null) {
                        Variables.geminiKeys.add(apiKey);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch Gemini API keys: " + databaseError.getMessage());
            }
        });

        // Add Claude keys loading
        DatabaseReference claudeKeysRef = FirebaseDatabase.getInstance()
                .getReference("apikeys")
                .child("claude");

        claudeKeysRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Variables.claudeKeys.clear();
                for (DataSnapshot keySnapshot : dataSnapshot.getChildren()) {
                    String apiKey = keySnapshot.getValue(String.class);
                    if (apiKey != null) {
                        Variables.claudeKeys.add(apiKey);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to fetch Claude API keys: " + databaseError.getMessage());
            }
        });
    }

    private void userDataListener(){
        // Check if this is a guest user
        if ("guest".equals(Variables.userUID)) {
            // Guest user already has Variables set, just continue with the app
            Log.d(TAG, "Guest user detected, using local variables");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            Variables.userUID = user.getUserId();
                            Variables.userEmail = user.getEmail();
                            Variables.userDisplayName = user.getUsername();
                            Variables.userAccountType = user.getAccountType();
                            Variables.userLanguage = user.getLanguage();
                            Variables.userTranslator = user.getTranslator();
                            
                            // If language is not set, redirect to LanguageSetupActivity
                            if (user.getLanguage() == null) {
                                Log.e(TAG, "User language not set");
                                Intent intent = new Intent(MainActivity.this, LanguageSetupActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }
                    } else {
                        // If user data doesn't exist, redirect to LanguageSetupActivity
                        Log.e(TAG, "User data doesn't exist in database");
                        Intent intent = new Intent(MainActivity.this, LanguageSetupActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error getting user: " + databaseError.getMessage());
                }
            });
        } else {
            // If user is null, redirect to WelcomeScreen
            Log.e(TAG, "Current user is null");
            Intent intent = new Intent(MainActivity.this, WelcomeScreen.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize translation mode from SharedPreferences
        TranslationModeManager.initializeFromPreferences(this);

        // Load API keys at startup
        loadApiKeys();

        // Remove the flag check that was causing the crash
        // Instead, just prevent going back
        if (isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)) {
            // App was started from launcher, clear any existing tasks
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        // Check if user data is available
        userDataListener();

        // Show BasicTranslationFragment directly (no tabs)
        basicTranslationFragment = new BasicTranslationFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.mainContentFrame, basicTranslationFragment)
            .commit();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Check if this is a guest user
        if ("guest".equals(Variables.userUID)) {
            // Guest user, just continue
            Log.d(TAG, "Guest user detected");
        } else if (mAuth.getCurrentUser() == null) {
            // User is not logged in, redirect to WelcomeScreen
            startActivity(new Intent(this, WelcomeScreen.class));
            finish();
        } else {
            // Reset tracking state for fresh app session and start listening for connection requests
            ConnectionRequestManager.getInstance().resetTrackingState();
            ConnectionRequestManager.getInstance().startListeningForRequests(this);
        }


    }

    public void openProfileFragment() {
        ProfileFragment profileFragment = new ProfileFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.mainContentFrame, profileFragment)
            .addToBackStack(null)
            .commit();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop listening for connection requests to prevent memory leaks
        ConnectionRequestManager.getInstance().stopListeningForRequests();
    }

}
