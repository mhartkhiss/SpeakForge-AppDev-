package com.example.appdev.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.RecognizerIntent;
import android.os.Bundle;
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
import com.example.appdev.R;
import com.example.appdev.classes.FetchUserField;
import com.example.appdev.classes.Translate;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceFragment extends Fragment {

    private static final int SPEECH_REQUEST_CODE = 1;

    private TextView textViewResult;
    private Spinner languageSpinner;
    private DatabaseReference userRef;
    private String selectedLanguage;

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
        FetchUserField.fetchUserField("targetLanguage", new FetchUserField.UserFieldListener() {
            @Override
            public void onFieldReceived(String fieldValue) {
                // Use the field value here
                // Set the default selected value on the spinner
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    // Find the spinner
                    Toast.makeText(requireContext(), fieldValue, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(DatabaseError databaseError) {
                // Handle error
            }
        });


        // Set up database reference
        userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        // Initialize views and listeners
        textViewResult = view.findViewById(R.id.recognizedTextView);
        languageSpinner = view.findViewById(R.id.languageSpinner);
        Button buttonStartSpeech = view.findViewById(R.id.startSpeakingButton);

        // Set up language spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Spinner item selected listener
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguage = parent.getItemAtPosition(position).toString();
                saveSelectedLanguage(selectedLanguage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle nothing selected event
            }
        });

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

    private void saveSelectedLanguage(String selectedLanguage) {
        userRef.child("targetLanguage").setValue(selectedLanguage);
    }
}

