package com.example.appdev;

public class Variables {

    public static final String djangoServer = "192.168.10.248:8000"; //change to your server's IP address
    //------------------------------------------------------------------------
    public static final String request = "http://"; //change to https if needed, django uses http by default
    public static final String loginURL = request + djangoServer + "/api/login/";
    public static final String registerURL = request + djangoServer + "/api/register/";
    public static final String translateURL = request + djangoServer + "/api/translate/";
    public static final String supportedLanguagesURL = request + djangoServer + "/api/get_supported_languages/";
    public static final String registerSuccess = "Registration Successful";
    public static final String loginSuccess = "Login Successful";
}
