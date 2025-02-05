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

public class Translation_Claude extends AsyncTask<String, Void, String> {
    private static final String TAG = "ClaudeTranslator";
    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";
    
    private String targetLanguage;
    private TranslationListener listener;

    public Translation_Claude(String targetLanguage, TranslationListener listener) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        
        // Check if we have any API keys
        if (Variables.claudeKeys.isEmpty()) {
            return "Error: No API keys available";
        }

        // Try each API key until successful or all keys are exhausted
        for (String apiKey : Variables.claudeKeys) {
            try {
                String result = attemptTranslation(inputText, apiKey);
                if (!result.startsWith("Error:")) {
                    return result;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error with API key: " + e.getMessage());
            }
        }
        
        return "Error: All translation attempts failed";
    }

    private String attemptTranslation(String inputText, String apiKey) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(CLAUDE_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", "2023-06-01");
            connection.setRequestProperty("content-type", "application/json");
            connection.setDoOutput(true);

            // Log the API key being used (first few characters)
            String maskedKey = apiKey.substring(0, Math.min(apiKey.length(), 5)) + "...";
            Log.d(TAG, "Attempting translation with API key: " + maskedKey);

            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            JSONArray messages = new JSONArray();
            
            // Create the prompt based on the openAiPrompt setting
            String prompt;
            if (Variables.openAiPrompt == 1) {
                prompt = String.format(
                    "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead. " +
                    "Translate to %s. Output ONLY the translation itself - no explanations, no language detection notes, no additional text. " +
                    "Preserve any slang or explicit words from the original text. Here is the text to translate: %s", 
                    Variables.userLanguage, targetLanguage, inputText);
            } else {
                prompt = String.format(
                    "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead. " +
                    "Translate to %s and provide exactly 3 numbered variations. Output ONLY the translations - no explanations, no language detection notes. " +
                    "Format: 1. [translation]\\n2. [translation]\\n3. [translation]. Here is the text to translate: %s", 
                    Variables.userLanguage, targetLanguage, inputText);
            }

            // Add user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            // Set the request parameters
            requestBody.put("model", "claude-3-sonnet-20240229");
            requestBody.put("max_tokens", 1024);
            requestBody.put("messages", messages);

            // Log the request body for debugging
            Log.d(TAG, "Request body: " + requestBody.toString());

            // Send the request
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(requestBody.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

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
                String responseStr = response.toString();
                Log.d(TAG, "API Response: " + responseStr);

                // Parse the response
                JSONObject jsonResponse = new JSONObject(responseStr);
                if (jsonResponse.has("content") && !jsonResponse.isNull("content")) {
                    JSONArray contents = jsonResponse.getJSONArray("content");
                    if (contents.length() > 0) {
                        JSONObject firstContent = contents.getJSONObject(0);
                        if (firstContent.has("text")) {
                            return firstContent.getString("text").trim();
                        }
                    }
                }
                
                Log.e(TAG, "Unexpected response structure: " + responseStr);
                return "Error: Unexpected response format";
            } else {
                String errorResponse = handleError(connection);
                Log.e(TAG, "Translation failed with error: " + errorResponse);
                return errorResponse;
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Translation error: " + e.getMessage(), e);
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