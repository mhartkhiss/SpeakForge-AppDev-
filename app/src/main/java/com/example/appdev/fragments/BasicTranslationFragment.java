package com.example.appdev.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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
import com.example.appdev.tasks.Translation;
import com.example.appdev.Variables;
import com.example.appdev.models.Languages;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.Locale;

public class BasicTranslationFragment extends Fragment {

    private static final int SPEECH_REQUEST_CODE = 1;
    private TextView textViewResult;
    private Spinner outputLanguageSelection;
    private TextInputEditText textInput;
    private TextInputLayout textInputLayout;
    private Button btnStartSpeech, btnTranslate;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // HIDING THE TEXTVIEW WHEN THE KEYBOARD IS OPEN
        rootView = inflater.inflate(R.layout.fragment_basictranslation, container, false);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();

                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    textViewResult.setVisibility(View.GONE);
                } else {
                    textViewResult.setVisibility(View.VISIBLE);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        textInput = view.findViewById(R.id.textInputEditText);
        textInputLayout = view.findViewById(R.id.textInputLayout);
        textViewResult = view.findViewById(R.id.txtTranslatedText);

        // Initialize Firebase
        FirebaseApp.initializeApp(requireContext());


        outputLanguageSelection = requireView().findViewById(R.id.languageSpinner);
        String[] languages = Languages.getLanguages().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outputLanguageSelection.setAdapter(adapter);

        setListeners(view);



    }

    private void setListeners(View view) {

        //VOICE-TO-TEXT TRANSLATION LISTENERS
        btnStartSpeech = view.findViewById(R.id.startSpeakingButton);
        btnStartSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognition();
            }
        });

        //TEXT-TO-TEXT TRANSLATION LISTENERS
        textInput = view.findViewById(R.id.textInputEditText);
        btnTranslate = view.findViewById(R.id.translateButton);
        btnTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                translateAnimation();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
                }
                String targetLanguage = BasicTranslationFragment.this.outputLanguageSelection.getSelectedItem().toString();

                Translation translationTask = new Translation(targetLanguage, new Translation.TranslationListener() {
                    @Override
                    public void onTranslationComplete(String translatedMessage) {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            String[] lines = translatedMessage.split("\n");
                            if (lines.length > 0) {
                                String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                                textViewResult.setText(firstLine);
                                textViewResult.setTextColor(getResources().getColor(R.color.black));
                                textViewResult.setTextSize(38);
                            }
                            textInput.setText("");
                            textInput.clearFocus();
                        }
                    }
                });
                translationTask.execute(textInput.getText().toString());
            }
        });

        textInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() > 0) {
                    btnStartSpeech.setVisibility(View.GONE);
                    btnTranslate.setVisibility(View.VISIBLE);
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textInputLayout.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, R.id.translateButton);
                    textInputLayout.setLayoutParams(params);
                } else {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textInputLayout.getLayoutParams();
                    params.addRule(RelativeLayout.ABOVE, R.id.startSpeakingButton);
                    textInputLayout.setLayoutParams(params);
                    btnStartSpeech.setVisibility(View.VISIBLE);
                    btnTranslate.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        textInput.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                String targetLanguage = this.outputLanguageSelection.getSelectedItem().toString();
                Translation translationTask = new Translation(targetLanguage, new Translation.TranslationListener() {
                    @Override
                    public void onTranslationComplete(String translatedMessage) {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            String[] lines = translatedMessage.split("\n");
                            if (lines.length > 0) {
                                String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                                textViewResult.setText(firstLine);
                                textViewResult.setTextColor(getResources().getColor(R.color.black));
                                textViewResult.setTextSize(38);
                            }
                        }
                    }
                });
                translationTask.execute(textInput.getText().toString());
                return true;
            }

            return false;
        });
    }

    private void translateAnimation(){

        textViewResult.setText("Translating");
        textViewResult.setTextColor(getResources().getColor(R.color.grey));
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            private int dotCount = 0;

            @Override
            public void run() {
                textViewResult.append(".");
                dotCount++;
                if (dotCount < 3) {
                    handler.postDelayed(this, 500);
                }
            }
        }, 500);

    }



    private void startSpeechRecognition() {

        Variables.userRef.child("targetLanguage").setValue(outputLanguageSelection.getSelectedItem().toString());
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
                String targetLanguage = this.outputLanguageSelection.getSelectedItem().toString();


                Translation translationTask = new Translation(targetLanguage, new Translation.TranslationListener() {
                    @Override
                    public void onTranslationComplete(String translatedMessage) {
                        if (!TextUtils.isEmpty(translatedMessage)) {
                            String[] lines = translatedMessage.split("\n");
                            if (lines.length > 0) {
                                String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                                textViewResult.setText(firstLine);
                                textViewResult.setTextColor(getResources().getColor(R.color.black));
                                textViewResult.setTextSize(38);
                            }
                        }
                    }
                });
                translationTask.execute(textToTranslate);
            }
        }
    }

}