package com.example.appdev;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 1;

    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FetchLanguages.fetchSupportedLanguages(this);

        textViewResult = findViewById(R.id.recognizedTextView);
        Button buttonStartSpeech = findViewById(R.id.startSpeakingButton);

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);

                Translate translate = new Translate(this);
                String textToTranslate = spokenText;
                Spinner spinner = findViewById(R.id.languageSpinner);
                String selectedLanguage = spinner.getSelectedItem().toString();

                translate.translateText(textToTranslate, selectedLanguage, new Translate.TranslateListener() {
                    @Override
                    public void onSuccess(String translatedText) {
                        textViewResult.setText(translatedText);
                    }

                    @Override
                    public void onError(VolleyError error) {
                        Toast.makeText(getApplicationContext(), "Error translating text", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

}



