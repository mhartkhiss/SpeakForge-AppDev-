package com.example.appdev.translators;

import android.content.Context;

public class TranslatorFactory {
    public static AsyncTask<String, Void, String> createTranslator(
            TranslatorType type, 
            String targetLanguage, 
            TranslationListener listener,
            Context context) {
        
        switch (type) {
            case GOOGLE:
                return new Translation_GoogleTranslate(context, targetLanguage, listener);
            case OPENAI:
                return new Translation_OpenAI(targetLanguage, listener);
            case DEEPSEEK:
                return new Translation_DeepSeekV3(targetLanguage, listener);
            case GPT4:
                return new Translation_GPT4(targetLanguage, listener);
            case GEMINI:
                return new Translation_Gemini(targetLanguage, listener);
            case CLAUDE:
                return new Translation_Claude(targetLanguage, listener);
            default:
                return new Translation_GoogleTranslate(context, targetLanguage, listener);
        }
    }

    public interface TranslationListener {
        void onTranslationComplete(String translatedText);
    }
} 