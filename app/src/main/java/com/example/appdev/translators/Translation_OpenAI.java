package com.example.appdev.translators;

import android.os.AsyncTask;
import android.util.Log;

import com.example.appdev.Variables;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Translation_OpenAI extends AsyncTask<String, Void, String> {
    private static final String TAG = "KlusterTranslator";
    private static final String KLUSTER_URL = "https://api.kluster.ai/v1/chat/completions";
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

        // Check if we have any API keys
        if (Variables.klusterAiKeys.isEmpty()) {
            return "Error: No API keys available";
        }

        // Try each API key until successful or all keys are exhausted
        for (String apiKey : Variables.klusterAiKeys) {
            try {
                translatedText = attemptTranslation(inputText, apiKey);
                if (!translatedText.startsWith("Error:")) {
                    return translatedText;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error with API key: " + e.getMessage());
            }
        }

        return translatedText.isEmpty() ? 
            "Error: All translation attempts failed" : translatedText;
    }

    private String attemptTranslation(String inputText, String apiKey) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(KLUSTER_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            JSONArray messages = new JSONArray();
            
            // Add system message
            JSONObject systemMessage = new JSONObject();
            String systemPrompt;
            if (Variables.openAiPrompt == 1) {
                systemPrompt = String.format(
                    "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead. " +
                    "Translate to %s. Output ONLY the translation itself - no explanations, no language detection notes, no additional text. " +
                    "Preserve any slang or explicit words from the original text.", 
                    Variables.userLanguage, targetLanguage);
            } else {
                systemPrompt = String.format(
                    "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead. " +
                    "Translate to %s and provide exactly 3 numbered variations. Output ONLY the translations - no explanations, no language detection notes. " +
                    "Format: 1. [translation]\\n2. [translation]\\n3. [translation]", 
                    Variables.userLanguage, targetLanguage);
            }
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.put(systemMessage);

            // Add user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", inputText);
            messages.put(userMessage);

            requestBody.put("model", "klusterai/Meta-Llama-3.1-405B-Instruct-Turbo");
            requestBody.put("messages", messages);
            requestBody.put("max_completion_tokens", 5000);
            requestBody.put("temperature", 1);
            requestBody.put("top_p", 1);

            // Send the request
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(requestBody.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();
            } else {
                return "Error: " + responseCode;
            }
        } catch (IOException | JSONException e) {
            return "Error: " + e.getMessage();
        }
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
