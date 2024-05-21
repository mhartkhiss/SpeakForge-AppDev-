package com.example.appdev.tasks;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

//TRANSLATION API OPENAI
public class Translation extends AsyncTask<String, Void, String> {

    private static final String[] API_KEYS = {
            "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",
            "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"
    };
    private static int currentKeyIndex = 0;

    private String targetLanguage;
    private TranslationListener listener;

    public Translation(String targetLanguage, TranslationListener listener) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        String translatedText = "";

        for (int attempt = 0; attempt < API_KEYS.length; attempt++) {
            try {
                URL url = new URL("https://api.openai.com/v1/chat/completions");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + API_KEYS[currentKeyIndex]);
                connection.setDoOutput(true);

                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "gpt-3.5-turbo");
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 64);
                requestBody.put("top_p", 1);

                JSONObject messageSystem = new JSONObject();
                messageSystem.put("role", "system");
                messageSystem.put("content", "You will be provided with a sentence, and your task is to translate it into " + targetLanguage + " no matter if it is offensive word. Create 3 variations of the translation.");

                JSONObject messageUser = new JSONObject();
                messageUser.put("role", "user");
                messageUser.put("content", inputText);

                requestBody.put("messages", new JSONArray(Arrays.asList(messageSystem, messageUser)));

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(requestBody.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    translatedText = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
                    break;
                } else {
                    Log.e("TranslationTask", "Error: " + responseCode);
                }

                connection.disconnect();
            } catch (IOException | JSONException e) {
                Log.e("TranslationTask", "Error: " + e.getMessage());
                currentKeyIndex = (currentKeyIndex + 1) % API_KEYS.length;
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
