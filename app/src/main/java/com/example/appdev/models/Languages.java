package com.example.appdev.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.example.appdev.Variables;

public class Languages {
    private String language_name;

    public Languages(String language_name) {
        this.language_name = language_name;
    }

    public String getName() {
        return language_name;
    }

    public static String getCurrentLanguage() {
        // Get current language from Variables.userLanguage
        return Variables.userLanguage != null ? Variables.userLanguage : "English";
    }

    // Method to get filtered languages (for translation target languages)
    public static List<String> getLanguages() {
        List<String> allLanguages = getAllLanguages();
        List<String> filteredLanguages = new ArrayList<>(allLanguages);
        filteredLanguages.remove(getCurrentLanguage());
        return filteredLanguages;
    }

    // Method to get all available languages (for language selection)
    public static List<String> getAllLanguages() {
        return Arrays.asList(
            "English", "Tagalog", "Bisaya"
        );
    }
}