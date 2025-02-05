package com.example.appdev.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Languages {
    private String language_name;
    private static String user1Language = "English";
    private static String user2Language = "Tagalog";

    public Languages(String language_name) {
        this.language_name = language_name;
    }

    public String getName() {
        return language_name;
    }

    // Get available languages for User 1 (excluding User 2's selection)
    public static List<String> getUser1Languages() {
        List<String> allLanguages = getAllLanguages();
        List<String> availableLanguages = new ArrayList<>(allLanguages);
        availableLanguages.remove(user2Language);
        return availableLanguages;
    }

    // Get available languages for User 2 (excluding User 1's selection)
    public static List<String> getUser2Languages() {
        List<String> allLanguages = getAllLanguages();
        List<String> availableLanguages = new ArrayList<>(allLanguages);
        availableLanguages.remove(user1Language);
        return availableLanguages;
    }

    // Update User 1's language selection
    public static void setUser1Language(String language) {
        if (!language.equals(user2Language)) {
            user1Language = language;
        }
    }

    // Update User 2's language selection
    public static void setUser2Language(String language) {
        if (!language.equals(user1Language)) {
            user2Language = language;
        }
    }

    // Get current language for User 1
    public static String getUser1Language() {
        return user1Language;
    }

    // Get current language for User 2
    public static String getUser2Language() {
        return user2Language;
    }

    // Method to get all available languages
    public static List<String> getAllLanguages() {
        return Arrays.asList(
            "English", "Tagalog", "Bisaya"
        );
    }
}