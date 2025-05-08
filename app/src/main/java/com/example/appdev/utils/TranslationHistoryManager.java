package com.example.appdev.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.appdev.Variables;
import com.example.appdev.models.TranslationHistory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TranslationHistoryManager {
    private static final String PREF_NAME = "translation_history";
    private static final String KEY_HISTORY_PREFIX = "history_list_";
    private final SharedPreferences preferences;
    private final Gson gson;
    private final String userId;

    public TranslationHistoryManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        
        // Get current user ID (or guest ID if user is a guest)
        userId = getCurrentUserId();
    }

    public void saveTranslation(TranslationHistory translation) {
        List<TranslationHistory> historyList = getHistory();
        historyList.add(0, translation); // Add new translation at the beginning
        
        // Limit history to 50 items
        if (historyList.size() > 50) {
            historyList = historyList.subList(0, 50);
        }
        
        String json = gson.toJson(historyList);
        preferences.edit().putString(getHistoryKey(), json).apply();
    }

    public List<TranslationHistory> getHistory() {
        String json = preferences.getString(getHistoryKey(), null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<TranslationHistory>>(){}.getType();
        List<TranslationHistory> historyList = gson.fromJson(json, type);
        return historyList != null ? historyList : new ArrayList<>();
    }

    public void clearHistory() {
        preferences.edit().remove(getHistoryKey()).apply();
    }
    
    /**
     * Gets the current user ID from Firebase Auth or Variables for guest users
     * @return User ID string
     */
    private String getCurrentUserId() {
        // Check if this is a guest user
        if ("guest".equals(Variables.userUID)) {
            return "guest";
        }
        
        // Try to get the current Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        
        // Fallback to Variables if Firebase user is null
        return Variables.userUID != null ? Variables.userUID : "guest";
    }
    
    /**
     * Gets the user-specific history key
     * @return The key for storing this user's translation history
     */
    private String getHistoryKey() {
        return KEY_HISTORY_PREFIX + userId;
    }
}