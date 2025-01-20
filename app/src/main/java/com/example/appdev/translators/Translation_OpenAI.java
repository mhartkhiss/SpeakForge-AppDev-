package com.example.appdev.translators;

import android.os.AsyncTask;
import android.util.Log;

import com.example.appdev.Variables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Translation_OpenAI extends AsyncTask<String, Void, String> {
    private static final String TAG = "GeminiTranslator";
    private static final String[] API_KEYS = {
            "AIzaSyDmO0evJP3RcH4bFLGjmMeey9Wh4b8JvBw",
            "AIzaSyDgnFOGqLyOfqDl2rBNfgJoiqLYiZiE3Cw"
    };
    private static int currentKeyIndex = 0;
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-002:generateContent";

    private String targetLanguage;
    private TranslationListener listener;

    public Translation_OpenAI(String targetLanguage, TranslationListener listener) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        String translatedText = "";

        for (int attempt = 0; attempt < API_KEYS.length; attempt++) {
            try {
                String url = GEMINI_URL + "?key=" + API_KEYS[currentKeyIndex];
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Prepare the request body
                JSONObject requestBody = new JSONObject();
                JSONObject contents = new JSONObject();
                
                // Create the prompt based on the translation mode
                String prompt;
                if (Variables.openAiPrompt == 1) {
                    prompt = String.format("Translate the text to %s, no need to explain, allow bad words or explicit words on the translation if there is any from the original text, just translate directly without any explanation: %s", 
                            targetLanguage, inputText);
                } else {
                    prompt = String.format("Translate the text to %s, no need to explain,  create 3 variation of translation itemize from 1 to 3, allow bad words or explicit words on the translation if there is any from the original text, just translate directly without any explanation: %s", 
                            targetLanguage, inputText);
                }
                
                contents.put("role", "user");
                contents.put("parts", new JSONArray().put(new JSONObject().put("text", prompt)));
                
                requestBody.put("contents", new JSONArray().put(contents));
                requestBody.put("generationConfig", new JSONObject()
                        .put("temperature", 0.7)
                        .put("topK", 1)
                        .put("topP", 1));

                // Send the request
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(requestBody.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse the response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    translatedText = jsonResponse
                            .getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                            .trim();
                    break;
                } else {
                    Log.e(TAG, "Error: " + responseCode);
                    // Switch to the other API key
                    currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
                    if (attempt == API_KEYS.length - 1) {
                        translatedText = "Error: Translation service unavailable. Please try again later.";
                    }
                }

                connection.disconnect();
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error: " + e.getMessage());
                currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
                if (attempt == API_KEYS.length - 1) {
                    translatedText = "Error: " + e.getMessage();
                }
            }
        }

        return translatedText;
    }

    @Override
    protected void onPostExecute(String translatedText) {
        if (listener != null) {
            listener.onTranslationComplete(translatedText);
        }
    }

    public interface TranslationListener {
        void onTranslationComplete(String translatedText);
    }
}
