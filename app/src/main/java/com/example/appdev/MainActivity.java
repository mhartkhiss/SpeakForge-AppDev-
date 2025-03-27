package com.example.appdev;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.appdev.adapters.TabAdapter;
import com.example.appdev.fragments.ChatFragment;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.fragments.BasicTranslationFragment;
import com.example.appdev.models.User;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

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

        // Load API keys at startup
        loadApiKeys();

        // Remove the flag check that was causing the crash
        // Instead, just prevent going back
        if (isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)) {
            // App was started from launcher, clear any existing tasks
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        userDataListener();

        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        // Create an adapter that returns a fragment for each tab
        TabAdapter adapter = new TabAdapter(getSupportFragmentManager());
        adapter.addFragment(new ProfileFragment(), "Profile");
        adapter.addFragment(new BasicTranslationFragment(), "");
        adapter.addFragment(new ChatFragment(), "Chat");

        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);

        // Connect the tab layout with the view pager
        tabLayout.setupWithViewPager(viewPager);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                
                // Check for guest user (either way)
                boolean isGuestUser = "guest".equals(Variables.userUID);
                if (!isGuestUser && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    isGuestUser = "guest".equals(Variables.userUID);
                }

                if ((position == 0 || position == 2) && isGuestUser) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Login Required")
                            .setMessage("You need to login to use this feature. Do you want to login now?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                // Sign out the current user and proceed to LoginActivity
                                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                    mAuth.signOut();
                                }
                                // Reset guest user variables
                                Variables.userUID = "";
                                Variables.userEmail = "";
                                Variables.userAccountType = "";
                                
                                // Clear guest user state in SharedPreferences
                                SharedPreferences prefs = getSharedPreferences(Variables.PREFS_NAME, MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean(Variables.PREF_IS_GUEST_USER, false);
                                editor.apply();
                                
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish(); // Finish the current activity to prevent returning to it
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                // Dismiss the dialog if user chooses not to login
                                dialog.dismiss();
                                tabLayout.getTabAt(1).select(); // Select the voice tab
                            })
                            .setOnCancelListener(dialog -> {
                                // Redirect the user to the voice tab when the dialog is canceled
                                tabLayout.getTabAt(1).select(); // Select the voice tab
                            })
                            .create()
                            .show();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Unused method but must be implemented
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Unused method but must be implemented

            }
        });


        // Add custom tab items with icons
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(R.layout.custom_tab_item);
                TextView tabText = tab.getCustomView().findViewById(R.id.tabText);
                ImageView tabIcon = tab.getCustomView().findViewById(R.id.tabIcon);
                tabText.setText(adapter.getPageTitle(i));
                switch (i) {
                    case 0:
                        tabIcon.setImageResource(R.drawable.ic_profile);
                        break;
                    case 1:
                        tabIcon.setImageResource(R.drawable.ic_translate_fragment);
                        break;
                    case 2:
                        tabIcon.setImageResource(R.drawable.ic_chat);
                        break;
                }

            }
        }
        // Select the "Voice" tab as the default tab
        TabLayout.Tab defaultTab = tabLayout.getTabAt(1); // Index of the "Voice" tab
        if (defaultTab != null) {
            defaultTab.select();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent going back
        moveTaskToBack(true);
    }

}
