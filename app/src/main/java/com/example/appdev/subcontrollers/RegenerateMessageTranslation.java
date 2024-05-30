package com.example.appdev.subcontrollers;

import android.text.TextUtils;
import android.util.Log;

import com.example.appdev.Variables;
import com.example.appdev.translators.Translation_OpenAI;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegenerateMessageTranslation {


    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference messagesRef = database.getReference("messages");

    private OnTranslationRegeneratedListener listener;


    public interface OnTranslationRegeneratedListener {
        void onTranslationRegenerated(String newTranslation);
    }

    public void setOnTranslationRegeneratedListener(OnTranslationRegeneratedListener listener) {
        this.listener = listener;
    }

    public void regenerate(String message, String messageId, String targetLanguage){

        // OpenAI
        Variables.openAiPrompt = 2;
        Translation_OpenAI translationOpenAITask = new Translation_OpenAI(targetLanguage, translatedMessage -> {
            if (!TextUtils.isEmpty(translatedMessage)) {
                storeTranslatedText(translatedMessage, messageId);
            }
        });
        translationOpenAITask.execute(message);

    }

    private void storeTranslatedText(String message, String messageId) {
        // Split the translated text by line breaks
        String[] lines = message.split("\n");

        if (lines.length >= 3) {
            String messageVar1 = lines[0];
            String messageVar2 = lines[1];
            String messageVar3 = lines[2];

            messageVar1 = removeQuotationMarks(messageVar1);
            messageVar2 = removeQuotationMarks(messageVar2);
            messageVar3 = removeQuotationMarks(messageVar3);

            messageVar1 = messageVar1.replaceFirst("^\\d+\\.\\s*", "");
            messageVar2 = messageVar2.replaceFirst("^\\d+\\.\\s*", "");
            messageVar3 = messageVar3.replaceFirst("^\\d+\\.\\s*", "");

            messagesRef.child(Variables.roomId).child(messageId).child("messageVar1").setValue(messageVar1);
            messagesRef.child(Variables.roomId).child(messageId).child("messageVar2").setValue(messageVar2);
            messagesRef.child(Variables.roomId).child(messageId).child("messageVar3").setValue(messageVar3);

            if (listener != null) {
                listener.onTranslationRegenerated(messageVar2); // or whichever message you want to use
            }
        } else {
            Log.e("RegenerateMessageTranslation", "Translated text does not have at least 3 lines");
        }

    }
    private String removeQuotationMarks(String text) {
        if (text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
