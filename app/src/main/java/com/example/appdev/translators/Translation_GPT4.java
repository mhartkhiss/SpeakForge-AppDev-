package com.example.appdev.translators;

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

import com.example.appdev.Variables;

public class Translation_GPT4 extends AsyncTask<String, Void, String> {
    private static final String TAG = "GPT4Translator";
    private static final String GPT4_URL = "https://chatgpt-42.p.rapidapi.com/gpt4";
    private static final String RAPID_API_KEY = "35708de7c0msh34b04d75ef9eab3p159aa0jsn078c5e1dbcca";
    private static final String RAPID_API_HOST = "chatgpt-42.p.rapidapi.com";
    
    private String targetLanguage;
    private TranslationListener listener;

    public Translation_GPT4(String targetLanguage, TranslationListener listener) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        try {
            return attemptTranslation(inputText);
        } catch (Exception e) {
            Log.e(TAG, "Translation error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private String attemptTranslation(String inputText) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(GPT4_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-rapidapi-key", RAPID_API_KEY);
            connection.setRequestProperty("x-rapidapi-host", RAPID_API_HOST);
            connection.setDoOutput(true);

            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            JSONArray messages = new JSONArray();
            
            // Add system message
            JSONObject systemMessage = new JSONObject();
            String systemPrompt = String.format(
                "You are a direct translator. Translate the following text from %s to %s. " +
                "Respond ONLY with the translation, no explanations or additional text.", 
                Variables.userLanguage, targetLanguage);
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.put(systemMessage);

            // Add user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", inputText);
            messages.put(userMessage);

            // The RapidAPI endpoint expects a simpler structure
            requestBody.put("messages", messages);
            requestBody.put("web_access", false);

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

                // Log the response for debugging
                Log.d(TAG, "API Response: " + response.toString());

                // Parse the response
                JSONObject jsonResponse = new JSONObject(response.toString());
                
                // Check if the response was successful
                if (jsonResponse.has("status") && jsonResponse.getBoolean("status")) {
                    if (jsonResponse.has("result")) {
                        return jsonResponse.getString("result").trim();
                    }
                }
                
                // If we get here, something unexpected happened
                Log.e(TAG, "Unexpected response structure: " + response.toString());
                return "Error: Unexpected response format";
            } else {
                return handleError(connection);
            }
        } catch (IOException | JSONException e) {
            return "Error: " + e.getMessage();
        }
    }

    private String handleError(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getErrorStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        String errorMessage = response.toString();
        Log.e(TAG, "Translation error response: " + errorMessage);
        Log.e(TAG, "Response code: " + connection.getResponseCode());
        Log.e(TAG, "Response message: " + connection.getResponseMessage());
        
        try {
            JSONObject errorJson = new JSONObject(errorMessage);
            if (errorJson.has("error")) {
                return "Error: " + errorJson.getString("error");
            } else if (errorJson.has("message")) {
                return "Error: " + errorJson.getString("message");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing error response", e);
        }
        
        return "Error: Translation failed (HTTP " + connection.getResponseCode() + ")";
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