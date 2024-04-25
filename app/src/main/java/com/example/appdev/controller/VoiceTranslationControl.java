package com.example.appdev.controller;


import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.appdev.classes.TranslationTask_OpenAI;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceTranslationControl {
    private static final int SPEECH_REQUEST_CODE = 1;

    private final Activity activity;
    private final Spinner spinner;

    public VoiceTranslationControl(Activity activity, Spinner spinner) {
        this.activity = activity;
        this.spinner = spinner;
    }

    public void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        activity.startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data, TranslationTask_OpenAI.TranslationListener translationListener) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);

                String textToTranslate = spokenText;
                String targetLanguage = spinner.getSelectedItem().toString();

                TranslationTask_OpenAI translationTask = new TranslationTask_OpenAI(targetLanguage, translationListener);
                translationTask.execute(textToTranslate);
            }
        }
    }
}

