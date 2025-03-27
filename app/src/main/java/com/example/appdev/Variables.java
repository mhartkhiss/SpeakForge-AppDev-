package com.example.appdev;

import java.util.ArrayList;
import java.util.List;

public class Variables {
    // Constant variables
    public static final String API_TRANSLATE_DB_URL = "https://speakforge-a52586b8a3f8.herokuapp.com/api/translate-db/";

    // SharedPreferences constants
    public static final String PREFS_NAME = "SpeakForgePrefs";
    public static final String PREF_IS_GUEST_USER = "isGuestUser";
    public static final String PREF_FORMAL_TRANSLATION_MODE = "formalTranslationMode";

    // Public variables
    public static String userUID = "";
    public static String userEmail = "";
    public static String userDisplayName = "";
    public static String userAccountType = "";
    public static String userLanguage = "";
    public static String userTranslator = "";
    public static String roomId = "";
    public static int openAiPrompt = 1;
    public static boolean isFormalTranslationMode = false;

    // API Keys
    public static List<String> klusterAiKeys = new ArrayList<>();
    public static List<String> deepseekKeys = new ArrayList<>();
    public static List<String> geminiKeys = new ArrayList<>();
    public static List<String> claudeKeys = new ArrayList<>();
}