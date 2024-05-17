package com.example.appdev;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Constants {

    public static final String guestUser = "a@gmail.com";
    public static final String guestUserPassword = "asdasd";
    public static final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
    public static final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
}
