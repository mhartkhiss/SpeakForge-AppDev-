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
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen);

        btnWLogin = findViewById(R.id.btnWLogin);
        btnWSkip = findViewById(R.id.btnWSkip);

        btnWLogin.setVisibility(View.GONE);
        btnWSkip.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(WelcomeScreen.this);

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    if (!Variables.guestUser.equals(user.getEmail())) {
                        CustomNotification.showNotification(WelcomeScreen.this, "Welcome back " + user.getEmail(), true);
                    }
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
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
                            }
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            CustomDialog.showDialog(WelcomeScreen.this, "Database Error", databaseError.getMessage());
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
                progressDialog.show();
                String email = Variables.guestUser;
                String password = Variables.guestUserPassword;

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(WelcomeScreen.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "signInWithEmail:success");
                                } else {
                                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                                    CustomDialog.showDialog(WelcomeScreen.this, "Authentication Failed", "Unable to sign in as guest");
                                }
                                progressDialog.dismiss();
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
