package com.example.appdev;

import java.util.ArrayList;
import java.util.List;

public class Variables {
    // Constant variables
    public static final String API_BASE_URL = "https://speakforge-a52586b8a3f8.herokuapp.com/api/";
    public static final String API_TRANSLATE_DB_URL = API_BASE_URL + "translate-db/";
    public static final String API_TRANSLATE_GROUP_URL = API_BASE_URL + "translate-group/";
    public static final String API_TRANSLATE_GROUP_CONTEXT_URL = API_BASE_URL + "translate-group-context/";
    public static final String API_REGENERATE_TRANSLATION_URL = API_BASE_URL + "regenerate-translation/";

    // SharedPreferences constants
    public static final String PREFS_NAME = "SpeakForgePrefs";
    public static final String PREF_IS_GUEST_USER = "isGuestUser";
    public static final String PREF_FORMAL_TRANSLATION_MODE = "formalTranslationMode";
    public static final String PREF_CONTEXT_AWARE_TRANSLATION = "contextAwareTranslation";
    public static final String PREF_CONTEXT_DEPTH = "contextDepth";

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
    public static boolean isContextAwareTranslation = true; // Default to enabled
    public static int contextDepth = 5; // Default context depth

    // API Keys
    public static List<String> klusterAiKeys = new ArrayList<>();
    public static List<String> deepseekKeys = new ArrayList<>();
    public static List<String> geminiKeys = new ArrayList<>();
    public static List<String> claudeKeys = new ArrayList<>();
}