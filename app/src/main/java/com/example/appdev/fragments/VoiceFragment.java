package com.example.appdev.fragments;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.VolleyError;
import com.example.appdev.classes.FetchLanguages;
import com.example.appdev.R; // Replace with your actual package name
import com.example.appdev.classes.FetchUserField;
import com.example.appdev.classes.TranslationTask_OpenAI;
import com.example.appdev.classes.Variables;
import com.example.appdev.models.Languages;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceFragment extends Fragment implements FetchLanguages.LanguagesListener{

    private static final int SPEECH_REQUEST_CODE = 1;

    private TextView textViewResult;

    private Spinner spinner, spinner2;
    private ArrayList<Languages> languageModels = new ArrayList<>();
    private String sourceLanguageCode, targetLanguageCode;

    private TextInputEditText textInputEditText;
    private TextInputLayout textInputLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        return inflater.inflate(R.layout.fragment_voice, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        textInputEditText = view.findViewById(R.id.textInputEditText);
        textInputLayout = view.findViewById(R.id.textInputLayout);

        // Initialize Firebase
        FirebaseApp.initializeApp(requireContext());

        setDefaultSpinnerValue();
        setListeners(view);

        spinner = requireView().findViewById(R.id.languageSpinner);
        //spinner2 = requireView().findViewById(R.id.languageSpinner2);
        String[] languages = {"English", "Tagalog", "Bisaya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        textInputEditText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                String targetLanguage = spinner.getSelectedItem().toString();
                TranslationTask_OpenAI translationTask = new TranslationTask_OpenAI(targetLanguage, new TranslationTask_OpenAI.TranslationListener() {
                    @Override
                    public void onTranslationComplete(String translatedMessage) {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            // Split the translatedMessage by newline character
                            String[] lines = translatedMessage.split("\n");
                            // Check if there is at least one line
                            if (lines.length > 0) {
                                // Remove the number from the first line
                                String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                                // Set the text of textViewResult to the first line
                                textViewResult.setText(firstLine);
                            }
                        }
                    }
                });
                translationTask.execute(textInputEditText.getText().toString());
                return true;
            }
            return false;
        });

        Button startSpeakingButton = view.findViewById(R.id.startSpeakingButton);
        Button translateButton = view.findViewById(R.id.translateButton);

        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(textInputEditText.getWindowToken(), 0);
                }
                String targetLanguage = spinner.getSelectedItem().toString();

                TranslationTask_OpenAI translationTask = new TranslationTask_OpenAI(targetLanguage, new TranslationTask_OpenAI.TranslationListener() {
                    @Override
                    public void onTranslationComplete(String translatedMessage) {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            String[] lines = translatedMessage.split("\n");
                            if (lines.length > 0) {
                                String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                                textViewResult.setText(firstLine);
                            }
                            textInputEditText.setText("");
                            textInputEditText.clearFocus();

                        }
                    }
                });
                translationTask.execute(textInputEditText.getText().toString());
            }
        });
        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // This method is called to notify you that, within s, the count characters
                // beginning at start are about to be replaced by new text with length after.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // This method is called to notify you that, within s, the count characters
                // beginning at start have just replaced old text that had length before.
                if(s.toString().trim().length() > 0) {
                    startSpeakingButton.setVisibility(View.GONE);
                    translateButton.setVisibility(View.VISIBLE);
                    // Get the current LayoutParams of the textInputLayout
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textInputLayout.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, R.id.translateButton);
                    textInputLayout.setLayoutParams(params);
                } else {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textInputLayout.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, R.id.startSpeakingButton);
                    textInputLayout.setLayoutParams(params);
                    startSpeakingButton.setVisibility(View.VISIBLE);
                    translateButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // This method is called to notify you that, somewhere within s, the text has
                // been changed.
            }
        });



    }

    public void onLanguagesReceived(ArrayList<Languages> languages) {
        // Handle received languages
        languageModels.addAll(languages); // Add the received languages to the list

        // Create an array of language names
        ArrayList<String> languageNames = new ArrayList<>();
        for (Languages language : languages) {
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
                Languages selectedLanguage = null;
                for (Languages language : languageModels) {
                    if (language.getName().equals(selectedLanguageName)) {
                        selectedLanguage = language;
                        break;
                    }
                }
                if (selectedLanguage != null) {
                    targetLanguageCode = selectedLanguage.getCode();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected language name
                String selectedLanguageName = languageNames.get(position);
                // Find the corresponding LanguageModel object
                Languages selectedLanguage = null;
                for (Languages language : languageModels) {
                    if (language.getName().equals(selectedLanguageName)) {
                        selectedLanguage = language;
                        break;
                    }
                }
                // If selectedLanguage is not null, you can get the code for the selected language
                if (selectedLanguage != null) {
                    sourceLanguageCode = selectedLanguage.getCode();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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

    }

    private void startSpeechRecognition() {

        Variables.userRef.child("targetLanguage").setValue(spinner.getSelectedItem().toString());
       // Variables.userRef.child("language").setValue(spinner2.getSelectedItem().toString());
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


                TranslationTask_OpenAI translationTask = new TranslationTask_OpenAI(targetLanguage, new TranslationTask_OpenAI.TranslationListener() {
                    @Override
                    public void onTranslationComplete(String translatedMessage) {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            String[] lines = translatedMessage.split("\n");
                            // Check if there is at least one line
                            if (lines.length > 0) {
                                // Remove the number from the first line
                                String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                                // Set the text of textViewResult to the first line
                                textViewResult.setText(firstLine);
                            }
                        }
                    }
                });
                translationTask.execute(textToTranslate);
            }
        }
    }

}