package com.example.appdev.translators;

import android.content.Context;
import android.os.AsyncTask;
import com.android.volley.VolleyError;
import com.example.appdev.Variables;

public class TranslatorFactory {
    public interface TranslationListener {
        void onTranslationComplete(String translatedText);
    }

    public static AsyncTask<String, Void, String> createTranslator(
            TranslatorType type, 
            String targetLanguage, 
            final TranslationListener listener,
            Context context) {
        
        // Pass the formal translation mode flag to translators
        boolean isFormalMode = Variables.isFormalTranslationMode;
        
        switch (type) {
            case GOOGLE:
                return new AsyncTask<String, Void, String>() {
                    @Override
                    protected String doInBackground(String... params) {
                        Translation_GoogleTranslate googleTranslator = new Translation_GoogleTranslate(context);
                        googleTranslator.translateText(params[0], targetLanguage, 
                            new Translation_GoogleTranslate.TranslateListener() {
                                @Override
                                public void onSuccess(String translatedText) {
                                    listener.onTranslationComplete(translatedText);
                                }

                                @Override
                                public void onError(VolleyError error) {
                                    listener.onTranslationComplete("Error: " + error.getMessage());
                                }
                            });
                        return null;
                    }
                };

            //case OPENAI:
            //    return new Translation_OpenAI(targetLanguage, 
            //        text -> listener.onTranslationComplete(text));

            case DEEPSEEK:
                return new Translation_DeepSeekV3(targetLanguage, 
                    text -> listener.onTranslationComplete(text), isFormalMode);

            //case GPT4:
            //    return new Translation_GPT4(targetLanguage, 
            //        text -> listener.onTranslationComplete(text));

            case GEMINI:
                return new Translation_Gemini(targetLanguage, 
                    text -> listener.onTranslationComplete(text), isFormalMode);

            case CLAUDE:
                return new Translation_Claude(targetLanguage, 
                    text -> listener.onTranslationComplete(text), isFormalMode);

            default:
                return createTranslator(TranslatorType.GOOGLE, targetLanguage, listener, context);
        }
    }
} 