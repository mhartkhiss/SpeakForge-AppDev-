package com.example.appdev;

import android.content.Intent;
import android.content.res.Resources;
import android.speech.RecognizerIntent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.intellij.lang.annotations.Language;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 1;


    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fetchSupportedLanguages();
        textViewResult = findViewById(R.id.recognizedTextView);
        Button buttonStartSpeech = findViewById(R.id.startSpeakingButton);

        buttonStartSpeech.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startSpeechRecognition();
            }
        });

    }
    private void fetchSupportedLanguages() {
        String url = Variables.supportedLanguagesURL;
        System.out.println("1");
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Parse the response
                        ArrayList<String> languageNames = new ArrayList<>();
                        try {
                            // Parse the response here
                            JSONArray jsonArray = response.getJSONArray("languages");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject languageObject = jsonArray.getJSONObject(i);
                                String name = languageObject.getString("name");
                                languageNames.add(name);
                            }
                            // Update UI with supported languages
                            updateLanguagesSpinner(languageNames);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                    }
                });

        queue.add(request);
    }


    private void updateLanguagesSpinner(ArrayList<String> languageNames) {
        Spinner spinner = findViewById(R.id.languageSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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
                //textViewResult.setText(spokenText);

                Translate translate = new Translate(this);

                String textToTranslate = spokenText;

                // Get the selected language from the spinner
                Spinner spinner = findViewById(R.id.languageSpinner);
                String selectedLanguage = spinner.getSelectedItem().toString();

                // Map the selected language to its corresponding language code
                //String targetLanguageCode = mapLanguageToCode(selectedLanguage);

                translate.translateText(textToTranslate, selectedLanguage, new Translate.TranslateListener() {
                    @Override
                    public void onSuccess(String translatedText) {
                        // Handle translated text here
                        // Update UI with translated text
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



