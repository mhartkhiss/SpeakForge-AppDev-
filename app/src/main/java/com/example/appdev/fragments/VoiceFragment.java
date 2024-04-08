package com.example.appdev.fragments;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.example.appdev.classes.FetchUserField;
import com.example.appdev.classes.Translate;
import com.example.appdev.classes.TranslationTask;
import com.example.appdev.classes.Variables;
import com.example.appdev.models.LanguageModel;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceFragment extends Fragment implements FetchLanguages.LanguagesListener{

    private static final int SPEECH_REQUEST_CODE = 1;

    private TextView textViewResult;

    private Spinner spinner, spinner2;
    private ArrayList<LanguageModel> languageModels = new ArrayList<>();
    private String sourceLanguageCode, targetLanguageCode;

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
        //FetchLanguages.fetchSupportedLanguages(requireContext(), this);
        setDefaultSpinnerValue();

        // Set listeners
        setListeners(view);

        spinner = requireView().findViewById(R.id.languageSpinner);
        //spinner2 = requireView().findViewById(R.id.languageSpinner2);
        String[] languages = {"English", "Tagalog", "Bisaya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        //spinner2.setAdapter(adapter);

    }

    public void onLanguagesReceived(ArrayList<LanguageModel> languages) {
        // Handle received languages
        languageModels.addAll(languages); // Add the received languages to the list

        // Create an array of language names
        ArrayList<String> languageNames = new ArrayList<>();
        for (LanguageModel language : languages) {
            languageNames.add(language.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner2.setAdapter(adapter);

        // Set up a listener to get the selected item
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected language name
                String selectedLanguageName = languageNames.get(position);
                // Find the corresponding LanguageModel object
                LanguageModel selectedLanguage = null;
                for (LanguageModel language : languageModels) {
                    if (language.getName().equals(selectedLanguageName)) {
                        selectedLanguage = language;
                        break;
                    }
                }
                // If selectedLanguage is not null, you can get the code for the selected language
                if (selectedLanguage != null) {
                    targetLanguageCode = selectedLanguage.getCode();
                    // Do whatever you need with the selected language code
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected
            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected language name
                String selectedLanguageName = languageNames.get(position);
                // Find the corresponding LanguageModel object
                LanguageModel selectedLanguage = null;
                for (LanguageModel language : languageModels) {
                    if (language.getName().equals(selectedLanguageName)) {
                        selectedLanguage = language;
                        break;
                    }
                }
                // If selectedLanguage is not null, you can get the code for the selected language
                if (selectedLanguage != null) {
                    sourceLanguageCode = selectedLanguage.getCode();
                    // Do whatever you need with the selected language code
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle the case where nothing is selected
            }
        });
    }


    @Override
    public void onError(VolleyError error) {
        // Handle error
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

    private void setDefaultSpinnerValue() {
        FetchUserField.fetchUserField("targetLanguage", new FetchUserField.UserFieldListener() {
            @Override
            public void onFieldReceived(String fieldValue) {
                Spinner spinner = getView().findViewById(R.id.languageSpinner);
                if (spinner != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
                    if (adapter != null) {
                        int position = adapter.getPosition(fieldValue);
                        if (position != -1) {
                            spinner.setSelection(position);
                        }
                    }
                }
            }

            @Override
            public void onError(DatabaseError databaseError) {
                // Handle error
            }
        });

        /*FetchUserField.fetchUserField("sourceLanguage", new FetchUserField.UserFieldListener() {
            @Override
            public void onFieldReceived(String fieldValue) {
                Spinner spinner = getView().findViewById(R.id.languageSpinner2);
                if (spinner != null) {
                    ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
                    if (adapter != null) {
                        int position = adapter.getPosition(fieldValue);
                        if (position != -1) {
                            spinner.setSelection(position);
                        }
                    }
                }
            }

            @Override
            public void onError(DatabaseError databaseError) {
                // Handle error
            }
        });*/


    }

    private void startSpeechRecognition() {

        Variables.userRef.child("targetLanguage").setValue(spinner.getSelectedItem().toString());
       // Variables.userRef.child("sourceLanguage").setValue(spinner2.getSelectedItem().toString());
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

                String textToTranslate = spokenText;
                String targetLanguage = spinner.getSelectedItem().toString();


                TranslationTask translationTask = new TranslationTask(targetLanguage, new TranslationTask.TranslationListener() {
                    @Override
                    public void onTranslationComplete(String translatedMessage) {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            textViewResult.setText(translatedMessage);
                        }
                    }
                });
                translationTask.execute(textToTranslate);
            }
        }
    }
}