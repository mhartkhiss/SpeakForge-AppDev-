package com.example.appdev;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.appdev.models.Languages;
import java.util.List;

public class LanguageSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_setup);

        // Get the container layout for buttons
        LinearLayout buttonContainer = findViewById(R.id.buttonContainer);

        // Get all available languages
        List<String> languages = Languages.getAllLanguages();

        // Create buttons dynamically
        for (String language : languages) {
            Button button = new Button(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(16, 8, 16, 8);
            button.setLayoutParams(params);
            
            button.setText(language);
            button.setBackgroundTintList(getColorStateList(R.color.light_blue));
            button.setTextColor(getResources().getColor(android.R.color.black));
            
            button.setOnClickListener(v -> updateSourceLanguage(language));
            
            buttonContainer.addView(button);
        }
    }

    private void updateSourceLanguage(String language) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
            userRef.child("language").setValue(language)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(LanguageSetupActivity.this, "Language updated successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LanguageSetupActivity.this, MainActivity.class));
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LanguageSetupActivity.this, "Failed to update language", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}