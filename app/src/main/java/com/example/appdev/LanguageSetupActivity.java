package com.example.appdev;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LanguageSetupActivity extends AppCompatActivity {

    Button [] btnLanguage = new Button[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_setup);

        btnLanguage[0] = findViewById(R.id.btnEnglish);
        btnLanguage[1] = findViewById(R.id.btnTagalog);
        btnLanguage[2] = findViewById(R.id.btnBisaya);

        for (int i = 0; i < btnLanguage.length; i++) {
            final int finalI = i;
            btnLanguage[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateSourceLanguage(btnLanguage[finalI].getText().toString());
                }
            });
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