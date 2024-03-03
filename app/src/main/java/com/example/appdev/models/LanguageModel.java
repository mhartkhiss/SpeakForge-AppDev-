package com.example.appdev.models;

public class LanguageModel {
    private String name;
    private String code;

    public LanguageModel(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }
}
