package com.example.appdev;

import java.util.ArrayList;
import java.util.List;

public class Variables {
    // Constant variables
    public static final String API_BASE_URL = "https://n7mgqpz5-8000.asse.devtunnels.ms/api/";
    public static final String API_TRANSLATE_DB_URL = API_BASE_URL + "translate-db/";
    public static final String API_TRANSLATE_BATCH_URL = API_BASE_URL + "translate-batch/";
    public static final String API_TRANSLATE_GROUP_URL = API_BASE_URL + "translate-group/";
    public static final String API_REGENERATE_TRANSLATION_URL = API_BASE_URL + "regenerate-translation/";

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