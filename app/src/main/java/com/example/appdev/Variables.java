package com.example.appdev;

public class Variables {

    public static final String djangoServer = "appdev.redirectme.net:25565"; //change to your server's IP address

    //------------------------------------------------------------------------
    public static final String request = "http://"; //change to https if needed, django uses http by default
    public static final String loginURL = request + djangoServer + "/api/login/";
    public static final String registerURL = request + djangoServer + "/api/register/";
    public static final String translateURL = request + djangoServer + "/api/translate/";
    public static final String registerSuccess = "Registration Successful";
    public static final String loginSuccess = "Login Successful";
}
