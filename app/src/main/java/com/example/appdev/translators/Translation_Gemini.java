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

public class Translation_Gemini extends AsyncTask<String, Void, String> {
    private static final String TAG = "GeminiTranslator";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";
    
    private String targetLanguage;
    private TranslationListener listener;

    public Translation_Gemini(String targetLanguage, TranslationListener listener) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        
        // Check if we have any API keys
        if (Variables.geminiKeys.isEmpty()) {
            return "Error: No API keys available";
        }

        // Try each API key until successful or all keys are exhausted
        for (String apiKey : Variables.geminiKeys) {
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
            String urlWithKey = GEMINI_URL + "?key=" + apiKey;
            HttpURLConnection connection = (HttpURLConnection) new URL(urlWithKey).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            
            // Add contents array with user message
            JSONArray contents = new JSONArray();
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            
            JSONArray userParts = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", inputText);
            userParts.put(textPart);
            
            userContent.put("parts", userParts);
            contents.put(userContent);
            requestBody.put("contents", contents);

            // Add system instruction
            JSONObject systemInstruction = new JSONObject();
            systemInstruction.put("role", "user");
            
            JSONArray systemParts = new JSONArray();
            JSONObject systemTextPart = new JSONObject();
            String systemPrompt = String.format(
                "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead. " +
                "Translate to %s. Output ONLY the translation itself - no explanations, no language detection notes, no additional text. " +
                "Preserve any slang or explicit words from the original text",
                Variables.userLanguage, targetLanguage);
            systemTextPart.put("text", systemPrompt);
            systemParts.put(systemTextPart);
            
            systemInstruction.put("parts", systemParts);
            requestBody.put("systemInstruction", systemInstruction);

            // Add generation config
            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 1);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 8192);
            generationConfig.put("responseMimeType", "text/plain");
            requestBody.put("generationConfig", generationConfig);

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
                JSONArray candidates = jsonResponse.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    if (parts.length() > 0) {
                        return parts.getJSONObject(0).getString("text").trim();
                    }
                }
                
                return "Error: No translation in response";
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
                JSONObject error = errorJson.getJSONObject("error");
                return "Error: " + error.getString("message");
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