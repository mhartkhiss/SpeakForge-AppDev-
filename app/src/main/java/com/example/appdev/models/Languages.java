package com.example.appdev.models;

import java.util.Arrays;
import java.util.List;

public class Languages {
    private String language_name;
    private String language_code;

    public Languages(String language_name, String language_code) {
        this.language_name = language_name;
        this.language_code = language_code;
    }

    public String getName() {
        return language_name;
    }

    public String getCode() {
        return language_code;
    }

    public static List<String> getLanguages() {
        return Arrays.asList("English", "Tagalog", "Bisaya");
    }
}