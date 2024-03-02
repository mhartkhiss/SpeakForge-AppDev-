package com.example.appdev.fragments;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.VolleyError;
import com.example.appdev.classes.FetchLanguages;
import com.example.appdev.R; // Replace with your actual package name
import com.example.appdev.classes.Translate;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceFragment extends Fragment {

    private static final int SPEECH_REQUEST_CODE = 1;

    private TextView textViewResult;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_voice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        FirebaseApp.initializeApp(requireContext());

        // Fetch supported languages
        FetchLanguages.fetchSupportedLanguages(requireContext());

        // Set listeners
        setListeners(view);
    }

    private void setListeners(View view) {
        textViewResult = view.findViewById(R.id.recognizedTextView);
        Button buttonStartSpeech = view.findViewById(R.id.startSpeakingButton);

        buttonStartSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognition();
            }
        });


    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == requireActivity().RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);

                Translate translate = new Translate(requireContext());
                String textToTranslate = spokenText;
                Spinner spinner = requireView().findViewById(R.id.languageSpinner);
                String selectedLanguage = spinner.getSelectedItem().toString();

                translate.translateText(textToTranslate, selectedLanguage, new Translate.TranslateListener() {
                    @Override
                    public void onSuccess(String translatedText) {
                        textViewResult.setText(translatedText);
                    }

                    @Override
                    public void onError(VolleyError error) {
                        Toast.makeText(requireContext(), "Error translating text", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
