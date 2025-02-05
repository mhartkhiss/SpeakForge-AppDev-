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

        AsyncTask<String, Void, String> translator = TranslatorFactory.createTranslator(
            translatorType,
            targetLanguage,
            translatedMessage -> {
                if (!TextUtils.isEmpty(translatedMessage)) {
                    // Split and store variations
                    String[] variations = translatedMessage.split("\n");
                    if (variations.length >= 3) {
                        storeTranslationVariations(variations, messageId);
                    } else {
                        // If we don't get 3 variations, just store the single translation
                        storeTranslatedText(translatedMessage, messageId);
                    }
                }
            },
            context
        );
        translator.execute(message);
    }

    private void storeTranslationVariations(String[] variations, String messageId) {
        String var1 = cleanVariation(variations[0]);
        String var2 = cleanVariation(variations[1]);
        String var3 = cleanVariation(variations[2]);

        // Store variations in Firebase
        messagesRef.child(Variables.roomId).child(messageId).child("messageVar1").setValue(var1);
        messagesRef.child(Variables.roomId).child(messageId).child("messageVar2").setValue(var2);
        messagesRef.child(Variables.roomId).child(messageId).child("messageVar3").setValue(var3);
        // Set the main message to var2 (middle variation)
        messagesRef.child(Variables.roomId).child(messageId).child("message").setValue(var2);

        if (listener != null) {
            listener.onTranslationRegenerated(var2);
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
        // Remove quotation marks
        text = removeQuotationMarks(text);
        // Remove numbered prefix (e.g., "1. ", "2. ")
        text = text.replaceFirst("^\\d+\\.\\s*", "");
        return text.trim();
    }

    private String removeQuotationMarks(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
