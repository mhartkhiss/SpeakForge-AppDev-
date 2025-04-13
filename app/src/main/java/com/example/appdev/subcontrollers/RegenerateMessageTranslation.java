package com.example.appdev.subcontrollers;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.example.appdev.Variables;
import com.example.appdev.translators.Translation_OpenAI;
import com.example.appdev.translators.Translation_DeepSeekV3;
import com.example.appdev.translators.Translation_GPT4;
import com.example.appdev.translators.Translation_Gemini;
import com.example.appdev.translators.Translation_Claude;
import com.example.appdev.utils.CustomDialog;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.appdev.translators.TranslatorFactory;
import com.example.appdev.translators.TranslatorType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import org.json.JSONObject;

public class RegenerateMessageTranslation {
    private static final String TAG = "RegenerateTranslation";
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference messagesRef = database.getReference("messages");
    private OnTranslationRegeneratedListener listener;
    private Context context;

    public interface OnTranslationRegeneratedListener {
        void onTranslationRegenerated(String newTranslation);
    }

    public RegenerateMessageTranslation(Context context) {
        this.context = context;
    }

    public void setOnTranslationRegeneratedListener(OnTranslationRegeneratedListener listener) {
        this.listener = listener;
    }

    public void regenerate(String message, String messageId, String targetLanguage) {
        // Always set to variation mode for regeneration
        Variables.openAiPrompt = 2;

        // Get the sender language for this message
        messagesRef.child(Variables.roomId).child(messageId).child("senderLanguage")
            .get().addOnCompleteListener(task -> {
                String senderLanguage;
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getValue() != null) {
                    senderLanguage = task.getResult().getValue(String.class);
                } else {
                    // Fallback to current user language if senderLanguage not found
                    senderLanguage = Variables.userLanguage;
                }

                // Use the server endpoint for regeneration instead of client-side translators
                try {
                    // Prepare the request body
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("text", message);
                    requestBody.put("source_language", senderLanguage);
                    requestBody.put("target_language", targetLanguage);
                    requestBody.put("variants", "multiple"); // Always get multiple variants
                    requestBody.put("model", Variables.userTranslator.toLowerCase());
                    requestBody.put("translation_mode", Variables.isFormalTranslationMode ? "formal" : "casual");
                    requestBody.put("room_id", Variables.roomId);
                    requestBody.put("message_id", messageId);
                    requestBody.put("is_group", false); // This is for direct messages

                    String apiUrl = Variables.API_REGENERATE_TRANSLATION_URL;
                    
                    // Make API request in background
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... voids) {
                            try {
                                URL url = new URL(apiUrl);
                                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Content-Type", "application/json");
                                conn.setDoOutput(true);

                                // Send request body
                                try (OutputStream os = conn.getOutputStream()) {
                                    byte[] input = requestBody.toString().getBytes("utf-8");
                                    os.write(input, 0, input.length);
                                }

                                // Check if request was successful
                                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    // Read the response but we don't need to parse it
                                    // The server updates Firebase directly
                                    return true;
                                }
                                
                                return false;
                            } catch (Exception e) {
                                Log.e(TAG, "Error making API request: " + e.getMessage());
                                return false;
                            }
                        }

                        @Override
                        protected void onPostExecute(Boolean success) {
                            if (success) {
                                // On success, get the updated translation to display
                                messagesRef.child(Variables.roomId).child(messageId)
                                    .child("translations").child("translation1")
                                    .get().addOnCompleteListener(task -> {
                                        if (task.isSuccessful() && task.getResult() != null && 
                                            task.getResult().getValue() != null) {
                                            String newTranslation = task.getResult().getValue(String.class);
                                            if (listener != null) {
                                                listener.onTranslationRegenerated(newTranslation);
                                            }
                                        } else {
                                            // If we can't get the new translation, inform the user
                                            if (listener != null) {
                                                listener.onTranslationRegenerated("Translation regeneration failed");
                                            }
                                        }
                                    });
                            } else {
                                if (listener != null) {
                                    listener.onTranslationRegenerated("Translation regeneration failed");
                                }
                            }
                        }
                    }.execute();
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error preparing regeneration request: " + e.getMessage());
                    if (listener != null) {
                        listener.onTranslationRegenerated("Failed to regenerate translation");
                    }
                }
            });
    }

    private void storeTranslationVariations(String[] variations, String messageId) {
        // Clean up variations and handle the format
        List<String> cleanVariations = new ArrayList<>();
        
        // First, handle case where variations are combined in one string
        if (variations.length == 1) {
            String[] splitVariations = variations[0]
                .split("(?=\\d+\\.)"); // Split on number followed by dot
            
            for (String variation : splitVariations) {
                String cleanVar = cleanVariation(variation);
                if (!cleanVar.isEmpty()) {
                    cleanVariations.add(cleanVar);
                }
            }
        } else {
            // Handle normal case where variations are already split
            for (String variation : variations) {
                if (variation.trim().isEmpty()) continue;
                String cleanVar = cleanVariation(variation);
                if (!cleanVar.isEmpty()) {
                    cleanVariations.add(cleanVar);
                }
            }
        }

        // Ensure we have at least one variation
        if (cleanVariations.isEmpty()) {
            return;
        }

        // Create a map for the translations
        Map<String, Object> translationsMap = new HashMap<>();
        
        // Store variations in Firebase
        if (cleanVariations.size() >= 3) {
            // Only create translation2 and translation3 if we have multiple variations
            translationsMap.put("translation1", cleanVariations.get(0));
            translationsMap.put("translation2", cleanVariations.get(1));
            translationsMap.put("translation3", cleanVariations.get(2));
            
            // Update the translations node in Firebase
            messagesRef.child(Variables.roomId).child(messageId).child("translations")
                .updateChildren(translationsMap);
            
            // Use the first variation as the main translation
            if (listener != null) {
                listener.onTranslationRegenerated(cleanVariations.get(0));
            }
        } else {
            // If we have fewer than 3 variations, just update translation1
            translationsMap.put("translation1", cleanVariations.get(0));
            
            // Update the translations node in Firebase
            messagesRef.child(Variables.roomId).child(messageId).child("translations")
                .updateChildren(translationsMap);
            
            if (listener != null) {
                listener.onTranslationRegenerated(cleanVariations.get(0));
            }
        }
    }

    private void storeTranslatedText(String translatedText, String messageId) {
        String cleanText = cleanVariation(translatedText);
        
        // Store as translation1 in the translations map
        Map<String, Object> translationsMap = new HashMap<>();
        translationsMap.put("translation1", cleanText);
        
        messagesRef.child(Variables.roomId).child(messageId).child("translations")
            .updateChildren(translationsMap);
        
        if (listener != null) {
            listener.onTranslationRegenerated(cleanText);
        }
    }

    private String cleanVariation(String text) {
        return text
            .replaceAll("^\\s*\\d+\\.\\s*", "") // Remove numbered prefixes
            .replaceAll("\\\\\\s*n", "") // Remove "\n" or "\ n"
            .replaceAll("^\"|\"$", "") // Remove surrounding quotes
            .trim();
    }
}
