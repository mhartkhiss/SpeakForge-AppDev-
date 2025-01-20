package com.example.appdev.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.appdev.models.TranslationHistory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TranslationHistoryManager {
    private static final String PREF_NAME = "translation_history";
    private static final String KEY_HISTORY = "history_list";
    private final SharedPreferences preferences;
    private final Gson gson;

    public TranslationHistoryManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveTranslation(TranslationHistory translation) {
        List<TranslationHistory> historyList = getHistory();
        historyList.add(0, translation); // Add new translation at the beginning
        
        // Limit history to 50 items
        if (historyList.size() > 50) {
            historyList = historyList.subList(0, 50);
        }
        
        String json = gson.toJson(historyList);
        preferences.edit().putString(KEY_HISTORY, json).apply();
    }

    public List<TranslationHistory> getHistory() {
        String json = preferences.getString(KEY_HISTORY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<TranslationHistory>>(){}.getType();
        List<TranslationHistory> historyList = gson.fromJson(json, type);
        return historyList != null ? historyList : new ArrayList<>();
    }

    public void clearHistory() {
        preferences.edit().remove(KEY_HISTORY).apply();
    }
} 