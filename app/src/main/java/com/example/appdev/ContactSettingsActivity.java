package com.example.appdev;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ContactSettingsActivity extends AppCompatActivity {

    private DatabaseReference contactSettingsRef;
    private String contactUid;
    private boolean isInitialLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_settings);

        // Get data from intent
        String username = getIntent().getStringExtra("username");
        String email = getIntent().getStringExtra("email");
        String language = getIntent().getStringExtra("language");
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");
        contactUid = getIntent().getStringExtra("userId");

        // Check if we have the required user ID
        if (contactUid == null) {
            CustomNotification.showNotification(this, "Error loading contact settings", false);
            finish();
            return;
        }

        // Initialize Firebase reference
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        contactSettingsRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUserUid)
                .child("contactsettings")
                .child(contactUid);

        // Initialize views
        TextView textViewUsername = findViewById(R.id.textViewUsername);
        TextView textViewEmail = findViewById(R.id.textViewEmail);
        TextView textViewLanguage = findViewById(R.id.textViewLanguage);
        TextView textViewLanguageLabel = findViewById(R.id.textViewLanguageLabel);
        ImageView imageViewProfile = findViewById(R.id.imageViewProfile);
        ImageView buttonBack = findViewById(R.id.buttonBack);
        SwitchMaterial switchTranslate = findViewById(R.id.switchTranslate);

        // Set data to views
        textViewUsername.setText(username);
        textViewEmail.setText(email);
        textViewLanguage.setText(language);
        textViewLanguageLabel.setText(getString(R.string.contact_language_label, username.toUpperCase()));

        // Load profile image
        if (profileImageUrl != null && !profileImageUrl.equals("none")) {
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.default_userpic)
                .into(imageViewProfile);
        }

        // Load saved settings from Firebase
        contactSettingsRef.child("translateMessages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    boolean translateEnabled = dataSnapshot.getValue(Boolean.class);
                    switchTranslate.setOnCheckedChangeListener(null);
                    switchTranslate.setChecked(translateEnabled);
                    setTranslateListener(switchTranslate);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Only show notification if the user is still logged in
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    CustomNotification.showNotification(ContactSettingsActivity.this,
                        "Failed to load settings", false);
                }
            }
        });

        // Initialize the switch listener
        setTranslateListener(switchTranslate);

        // Set back button click listener
        buttonBack.setOnClickListener(v -> finish());
    }

    private void setTranslateListener(SwitchMaterial switchTranslate) {
        switchTranslate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            contactSettingsRef.child("translateMessages").setValue(isChecked)
                .addOnSuccessListener(aVoid -> {
                    if (!isInitialLoad) {
                        CustomNotification.showNotification(ContactSettingsActivity.this,
                            isChecked ? "Translation enabled" : "Translation disabled", 
                            true);
                    }
                    isInitialLoad = false;
                })
                .addOnFailureListener(e -> {
                    switchTranslate.setChecked(!isChecked);
                    CustomNotification.showNotification(ContactSettingsActivity.this,
                        "Failed to save setting", 
                        false);
                });
        });
    }
} 