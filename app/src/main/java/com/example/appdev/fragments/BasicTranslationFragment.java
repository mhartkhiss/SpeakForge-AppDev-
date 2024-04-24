package com.example.appdev.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appdev.R;
import com.example.appdev.classes.TranslationTask_OpenAI;
import com.example.appdev.controller.VoiceTranslationControl;
import com.example.appdev.models.Languages;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;

public class BasicTranslationFragment extends Fragment {

    private TextView textViewResult;

    private Spinner languageSelect;
    private ArrayList<Languages> languageModels = new ArrayList<>();

    private TextInputEditText textInputEditText;
    private TextInputLayout textInputLayout;

    private VoiceTranslationControl voiceTranslationControl;

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
        FirebaseApp.initializeApp(requireContext());

        setListeners(view);

        languageSelect = requireView().findViewById(R.id.languageSpinner);
        String[] languages = Languages.getLanguages().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSelect.setAdapter(adapter);

        voiceTranslationControl = new VoiceTranslationControl(requireActivity(), languageSelect);
    }

    private void setListeners(View view) {
        textViewResult = view.findViewById(R.id.recognizedTextView);
        Button buttonStartSpeech = view.findViewById(R.id.startSpeakingButton);
        Button translateButton = view.findViewById(R.id.translateButton);

        buttonStartSpeech.setOnClickListener(v -> voiceTranslationControl.startSpeechRecognition());

        translateButton.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(textInputEditText.getWindowToken(), 0);
            }
            String targetLanguage = languageSelect.getSelectedItem().toString();

            TranslationTask_OpenAI translationTask = new TranslationTask_OpenAI(targetLanguage, translatedMessage -> {
                if (!TextUtils.isEmpty(translatedMessage)) {
                    String[] lines = translatedMessage.split("\n");
                    if (lines.length > 0) {
                        String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                        textViewResult.setText(firstLine);
                    }
                    textInputEditText.setText("");
                    textInputEditText.clearFocus();
                }
            });
            translationTask.execute(textInputEditText.getText().toString());
        });

        textInputEditText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                String targetLanguage = languageSelect.getSelectedItem().toString();
                TranslationTask_OpenAI translationTask = new TranslationTask_OpenAI(targetLanguage, translatedMessage -> {
                    if (!TextUtils.isEmpty(translatedMessage)) {
                        String[] lines = translatedMessage.split("\n");
                        if (lines.length > 0) {
                            String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                            textViewResult.setText(firstLine);
                        }
                    }
                });
                translationTask.execute(textInputEditText.getText().toString());
                return true;
            }
            return false;
        });

        textInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() > 0) {
                    buttonStartSpeech.setVisibility(View.GONE);
                    translateButton.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textInputLayout.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, R.id.translateButton);
                    textInputLayout.setLayoutParams(params);
                } else {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textInputLayout.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, R.id.startSpeakingButton);
                    textInputLayout.setLayoutParams(params);
                    buttonStartSpeech.setVisibility(View.VISIBLE);
                    translateButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        voiceTranslationControl.onActivityResult(requestCode, resultCode, data, translatedMessage -> {
            if (!TextUtils.isEmpty(translatedMessage)) {
                String[] lines = translatedMessage.split("\n");
                if (lines.length > 0) {
                    String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                    textViewResult.setText(firstLine);
                }
            }
        });
    }

}

