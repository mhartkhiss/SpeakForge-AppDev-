package com.example.appdev.translators;

import com.example.appdev.R;

public enum TranslatorType {
    GOOGLE("google", "Google Translate", R.drawable.translator_icon_google),
    //OPENAI("openai", "OpenAI", R.drawable.translator_icon_openai),
    CLAUDE("claude", "Claude", R.drawable.translator_icon_claude),
    DEEPSEEK("deepseek", "DeepSeek", R.drawable.translator_icon_deepseek),
    //GPT4("gpt4", "GPT-4", R.drawable.translator_icon_gpt4),
    GEMINI("gemini", "Gemini", R.drawable.translator_icon_gemini);


    private final String id;
    private final String displayName;
    private final int iconResourceId;

    TranslatorType(String id, String displayName, int iconResourceId) {
        this.id = id;
        this.displayName = displayName;
        this.iconResourceId = iconResourceId;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getIconResourceId() { return iconResourceId; }

    public static TranslatorType fromId(String id) {
        for (TranslatorType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return GOOGLE; // default
    }
} 