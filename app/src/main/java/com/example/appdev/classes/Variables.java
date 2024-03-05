package com.example.appdev.classes;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Variables {

    public static final String djangoServer = "appdev.redirectme.net:25565";
    public static final String djangoServer2 = "appdev.redirectme.net:24454";
    //------------------------------------------------------------------------
    public static final String request = "http://"; //change to https if needed, django uses http by default
    public static final String loginURL = request + djangoServer + "/api/login/";
    public static final String registerURL = request + djangoServer + "/api/register/";
    public static final String translateURL = request + djangoServer + "/api/translate/";
    public static final String translateURL2 = request + djangoServer + "/translate/";
    public static final String supportedLanguagesURL = request + djangoServer + "/api/get_supported_languages/";
    public static final String registerSuccess = "Registration Successful";
    public static final String loginSuccess = "Login Successful";
    public static final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
}
