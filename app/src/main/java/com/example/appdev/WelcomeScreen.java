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

import com.example.appdev.classes.Variables;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        btnWLogin = findViewById(R.id.btnWLogin);
        btnWSkip = findViewById(R.id.btnWSkip);

        btnWLogin.setVisibility(View.GONE);
        btnWSkip.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    if (!Variables.guestUser.equals(user.getEmail())) {
                    Toast.makeText(WelcomeScreen.this, "Welcome back " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    }
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid());
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String language = dataSnapshot.child("language").getValue(String.class);
                                if (language != null) {
                                    // User has the sourceLanguage field, start MainActivity
                                    startActivity(new Intent(WelcomeScreen.this, MainActivity.class));
                                } else {
                                    // User does not have the sourceLanguage field, start LanguageSetupActivity
                                    startActivity(new Intent(WelcomeScreen.this, LanguageSetupActivity.class));
                                }
                            }
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle database error
                            Toast.makeText(WelcomeScreen.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    btnWLogin.setVisibility(View.VISIBLE);
                    btnWSkip.setVisibility(View.VISIBLE);
                }
            }
        };

        btnWLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeScreen.this, LoginActivity.class));
            }
        });

        btnWSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform sign in with provided credentials
                String email = Variables.guestUser;
                String password = Variables.guestUserPassword;

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(WelcomeScreen.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, navigate to MainActivity
                                    Log.d(TAG, "signInWithEmail:success");
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    startActivity(new Intent(WelcomeScreen.this, MainActivity.class));
                                    finish(); // Finish the WelcomeScreen activity
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(WelcomeScreen.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

    }

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
}
