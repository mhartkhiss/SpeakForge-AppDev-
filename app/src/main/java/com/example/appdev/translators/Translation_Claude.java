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
import androidx.collection.LruCache;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class Translation_Claude extends AsyncTask<String, Void, String> {
    private static final String TAG = "ClaudeTranslator";
    private static final String CLAUDE_URL = "https://api.anthropic.com/v1/messages";
    
    private String targetLanguage;
    private TranslationListener listener;
    private boolean isFormalMode;
    private static final int CACHE_SIZE = 100; // Cache size of 100 entries
    private static LruCache<String, String> translationCache = new LruCache<>(CACHE_SIZE);

    public Translation_Claude(String targetLanguage, TranslationListener listener) {
        this(targetLanguage, listener, false);
    }

    public Translation_Claude(String targetLanguage, TranslationListener listener, boolean isFormalMode) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
        this.isFormalMode = isFormalMode;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        
        if (Variables.claudeKeys.isEmpty()) {
            return "Error: No API keys available";
        }

        // Generate cache key based on input text and target language
        String cacheKey = generateCacheKey(inputText, targetLanguage);
        
        // Check cache first
        String cachedTranslation = translationCache.get(cacheKey);
        if (cachedTranslation != null) {
            Log.d(TAG, "Cache hit for: " + cacheKey);
            return cachedTranslation;
        }

        // If not in cache, proceed with API translation
        for (String apiKey : Variables.claudeKeys) {
            try {
                String result = attemptTranslation(inputText, apiKey);
                if (!result.startsWith("Error:")) {
                    // Store successful translation in cache
                    translationCache.put(cacheKey, result);
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

            JSONObject requestBody = new JSONObject();
            
            // Set model and parameters
            requestBody.put("model", "claude-3-5-sonnet-20241022");
            requestBody.put("max_tokens", 8192);
            requestBody.put("temperature", Variables.openAiPrompt == 1 ? 0 : 0.7);

            // Create system message array with caching
            JSONArray systemMessages = new JSONArray();
            
            // Add the main system prompt
            JSONObject mainPrompt = new JSONObject();
            mainPrompt.put("type", "text");
            mainPrompt.put("text", String.format(
                "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead.",
                Variables.userLanguage));
            systemMessages.put(mainPrompt);

            // Add the cached translation instructions
            JSONObject translationInstructions = new JSONObject();
            translationInstructions.put("type", "text");
            if (Variables.openAiPrompt == 1) {
                String formalityInstruction = isFormalMode ? 
                    "Use formal language appropriate for academic or professional contexts. Avoid slang, contractions, casual expressions, and filter out any profanity or inappropriate language completely." : 
                    "Preserve any slang or explicit words from the original text.";
                
                translationInstructions.put("text", String.format(
                    "Translate to %s. Output ONLY the translation itself - no explanations, no language detection notes, no additional text. %s", 
                    targetLanguage, formalityInstruction));
            } else {
                String formalityInstruction = isFormalMode ? 
                    "Use formal language appropriate for academic or professional contexts in all variations. Filter out any profanity or inappropriate language completely." : 
                    "Include a mix of formality levels in your variations.";
                
                translationInstructions.put("text", String.format(
                    "Translate to %s and provide exactly 3 numbered variations. Output ONLY the translations - no explanations, no language detection notes. %s " +
                    "Format: 1. [translation]\\n2. [translation]\\n3. [translation]",
                    targetLanguage, formalityInstruction));
            }
            
            // Add cache control
            JSONObject cacheControl = new JSONObject();
            cacheControl.put("type", "ephemeral");
            translationInstructions.put("cache_control", cacheControl);
            
            systemMessages.put(translationInstructions);
            
            // Add system messages array to request body
            requestBody.put("system", systemMessages);

            // Set messages array
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", inputText);
            messages.put(userMessage);
            requestBody.put("messages", messages);

            // Log request for debugging
            Log.d(TAG, "Request body: " + requestBody.toString());

            // Send request
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(requestBody.toString().getBytes());
            outputStream.flush();
            outputStream.close();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String responseStr = response.toString();
                Log.d(TAG, "API Response: " + responseStr);

                // Parse response
                JSONObject jsonResponse = new JSONObject(responseStr);
                if (jsonResponse.has("content")) {
                    return jsonResponse.getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text")
                        .trim();
                }
                
                Log.e(TAG, "Unexpected response structure: " + responseStr);
                return "Error: Unexpected response format";
            } else {
                return handleError(connection);
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
        Log.e(TAG, "Error response: " + errorMessage);
        Log.e(TAG, "Response code: " + connection.getResponseCode());
        
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

    private String generateCacheKey(String inputText, String targetLanguage) {
        try {
            String combined = inputText.toLowerCase(Locale.ROOT) + "|" + 
                            targetLanguage.toLowerCase(Locale.ROOT) + "|" +
                            (Variables.openAiPrompt == 1 ? "single" : "multiple") + "|" +
                            (isFormalMode ? "formal" : "casual");
            
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(combined.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error generating cache key", e);
            // Fallback to a simpler key if MD5 is not available
            return (inputText + targetLanguage + Variables.openAiPrompt + isFormalMode).hashCode() + "";
        }
    }
} 