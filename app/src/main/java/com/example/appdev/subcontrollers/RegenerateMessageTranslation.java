package com.example.appdev.subcontrollers;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.example.appdev.Variables;
import com.example.appdev.translators.Translation_OpenAI;
import com.example.appdev.translators.Translation_DeepSeekV3;
import com.example.appdev.translators.Translation_GPT4;
import com.example.appdev.translators.Translation_Gemini;
import com.example.appdev.translators.Translation_Claude;
import com.example.appdev.utils.CustomDialog;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.appdev.translators.TranslatorFactory;
import com.example.appdev.translators.TranslatorType;

import java.util.ArrayList;
import java.util.List;

public class RegenerateMessageTranslation {
    private static final String TAG = "RegenerateTranslation";
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference messagesRef = database.getReference("messages");
    private OnTranslationRegeneratedListener listener;
    private Context context;

    public interface OnTranslationRegeneratedListener {
        void onTranslationRegenerated(String newTranslation);
    }

    public RegenerateMessageTranslation(Context context) {
        this.context = context;
    }

    public void setOnTranslationRegeneratedListener(OnTranslationRegeneratedListener listener) {
        this.listener = listener;
    }

    public void regenerate(String message, String messageId, String targetLanguage) {
        // Always set to variation mode for regeneration
        Variables.openAiPrompt = 2;

        TranslatorType translatorType = TranslatorType.fromId(Variables.userTranslator);
        
        if (translatorType == TranslatorType.GOOGLE) {
            Log.e(TAG, "Regeneration not supported for Google Translate");
            CustomDialog.showDialog(
                context,
                "Feature Not Available",
                "Translation variations are not available with Google Translate. Please switch to an AI-powered translator to use this feature."
            );
            return;
        }

        // Get the source language for this message
        messagesRef.child(Variables.roomId).child(messageId).child("sourceLanguage")
            .get().addOnCompleteListener(task -> {
                String sourceLanguage;
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getValue() != null) {
                    sourceLanguage = task.getResult().getValue(String.class);
                } else {
                    // Fallback to current user language if sourceLanguage not found
                    sourceLanguage = Variables.userLanguage;
                }

                AsyncTask<String, Void, String> translator = TranslatorFactory.createTranslator(
                    translatorType,
                    targetLanguage,
                    translatedMessage -> {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            // Split and store variations
                            String[] variations = translatedMessage.split("\n");
                            storeTranslationVariations(variations, messageId);
                        }
                    },
                    context
                );
                translator.execute(message);
            });
    }

    private void storeTranslationVariations(String[] variations, String messageId) {
        // Clean up variations and handle the format
        List<String> cleanVariations = new ArrayList<>();
        
        // First, handle case where variations are combined in one string
        if (variations.length == 1) {
            String[] splitVariations = variations[0]
                .split("(?=\\d+\\.)"); // Split on number followed by dot
            
            for (String variation : splitVariations) {
                String cleanVar = cleanVariation(variation);
                if (!cleanVar.isEmpty()) {
                    cleanVariations.add(cleanVar);
                }
            }
        } else {
            // Handle normal case where variations are already split
            for (String variation : variations) {
                if (variation.trim().isEmpty()) continue;
                String cleanVar = cleanVariation(variation);
                if (!cleanVar.isEmpty()) {
                    cleanVariations.add(cleanVar);
                }
            }
        }

        // Ensure we have at least one variation
        if (cleanVariations.isEmpty()) {
            return;
        }

        // Store variations in Firebase
        if (cleanVariations.size() >= 3) {
            messagesRef.child(Variables.roomId).child(messageId).child("messageVar1")
                .setValue(cleanVariations.get(0));
            messagesRef.child(Variables.roomId).child(messageId).child("messageVar2")
                .setValue(cleanVariations.get(1));
            messagesRef.child(Variables.roomId).child(messageId).child("messageVar3")
                .setValue(cleanVariations.get(2));
            // Set the main message to var2 (middle variation)
            messagesRef.child(Variables.roomId).child(messageId).child("message")
                .setValue(cleanVariations.get(1));

            if (listener != null) {
                listener.onTranslationRegenerated(cleanVariations.get(1));
            }
        } else {
            // If we don't have 3 variations, just use the first one
            String translation = cleanVariations.get(0);
            storeTranslatedText(translation, messageId);
        }
    }

    private void storeTranslatedText(String translatedText, String messageId) {
        String cleanText = cleanVariation(translatedText);
        messagesRef.child(Variables.roomId).child(messageId).child("message").setValue(cleanText);
        
        if (listener != null) {
            listener.onTranslationRegenerated(cleanText);
        }
    }

    private String cleanVariation(String text) {
        return text
            .replaceAll("^\\s*\\d+\\.\\s*", "") // Remove numbered prefixes
            .replaceAll("\\\\\\s*n", "") // Remove "\n" or "\ n"
            .replaceAll("^\"|\"$", "") // Remove surrounding quotes
            .trim();
    }
}
