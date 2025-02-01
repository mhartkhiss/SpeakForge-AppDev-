package com.example.appdev;

import java.util.ArrayList;
import java.util.List;

public class Variables {
    // Constant variables
    public static final String guestUser = "a@gmail.com";
    public static final String guestUserPassword = "asdasd";
    public static final String djangoServer = "appdev.redirectme.net:25565";
    public static final String request = "http://";
    public static final String translateURL = request + djangoServer + "/translate/";


    // Public variables
    public static String userUID = "";
    public static String userEmail = "";
    public static String userDisplayName = "";
    public static String userAccountType = "";
    public static String userLanguage = "";
    public static String userTranslator = "";
    public static String roomId = "";
    public static int openAiPrompt = 1;
    public static String pendingTranslation = "";

    // API Keys
    public static List<String> klusterAiKeys = new ArrayList<>();
    public static List<String> deepseekKeys = new ArrayList<>();
}