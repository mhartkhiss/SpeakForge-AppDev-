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

public class Translation_DeepSeekV3 extends AsyncTask<String, Void, String> {
    private static final String TAG = "DeepSeekTranslator";
    private static final String DEEPSEEK_URL = "https://api.deepseek.com/chat/completions";
    private String targetLanguage;
    private TranslationListener listener;
    private boolean isFormalMode;

    public Translation_DeepSeekV3(String targetLanguage, TranslationListener listener) {
        this(targetLanguage, listener, false);
    }

    public Translation_DeepSeekV3(String targetLanguage, TranslationListener listener, boolean isFormalMode) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
        this.isFormalMode = isFormalMode;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        String translatedText = "";

        // Check if we have any API keys
        if (Variables.deepseekKeys.isEmpty()) {
            return "Error: No API keys available";
        }

        // Try each API key until successful or all keys are exhausted
        for (String apiKey : Variables.deepseekKeys) {
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
            HttpURLConnection connection = (HttpURLConnection) new URL(DEEPSEEK_URL).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            JSONArray messages = new JSONArray();
            
            // Add system message
            JSONObject systemMessage = new JSONObject();
            String formalityInstruction = isFormalMode ? 
                "Use formal language appropriate for academic or professional contexts. Avoid slang, contractions, casual expressions, and filter out any profanity or inappropriate language completely." : 
                "Preserve any slang or explicit words from the original text.";
            
            String systemPrompt;
            if (Variables.openAiPrompt == 1) {
                systemPrompt = String.format(
                    "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead. " +
                    "Translate to %s. Output ONLY the translation itself - no explanations, no language detection notes, no additional text. " +
                    "%s", 
                    Variables.userLanguage, targetLanguage, formalityInstruction);
            } else {
                String variationInstruction = isFormalMode ? 
                    "Use formal language appropriate for academic or professional contexts in all variations. Filter out any profanity or inappropriate language completely." : 
                    "Include a mix of formality levels in your variations.";
                
                systemPrompt = String.format(
                    "You are a direct translator. If the input is not in %s, silently detect the actual language and translate from that language instead. " +
                    "Translate to %s and provide exactly 3 numbered variations. Output ONLY the translations - no explanations, no language detection notes. " +
                    "%s Format: 1. [translation]\\n2. [translation]\\n3. [translation]", 
                    Variables.userLanguage, targetLanguage, variationInstruction);
            }
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.put(systemMessage);

            // Add user message
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", inputText);
            messages.put(userMessage);

            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("stream", false);

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
                String content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim();

                // Clean the response by removing content between <think> tags
                content = cleanThinkTags(content);

                return content;
            } else {
                return "Error: " + responseCode;
            }
        } catch (IOException | JSONException e) {
            return "Error: " + e.getMessage();
        }
    }

    private String cleanThinkTags(String text) {
        // Remove everything between <think> and </think> tags
        String cleaned = text.replaceAll("(?s)<think>.*?</think>", "").trim();
        
        // Remove any remaining <think> or </think> tags just in case
        cleaned = cleaned.replaceAll("</?think>", "").trim();
        
        // Remove any double newlines that might have been created
        cleaned = cleaned.replaceAll("\\n\\s*\\n", "\n").trim();
        
        return cleaned;
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