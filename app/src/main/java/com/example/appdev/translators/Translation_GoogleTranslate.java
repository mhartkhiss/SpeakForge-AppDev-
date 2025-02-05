package com.example.appdev.translators;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;

public class Translation_GoogleTranslate {

    private static final String TAG = "GoogleTranslate";
    private static final String CLOUD_TRANSLATE_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String API_KEY = "AIzaSyBoxuz9MGqL0betnT6YR_zYITr74Qx9QX4";

    public interface TranslateListener {
        void onSuccess(String translatedText);
        void onError(VolleyError error);
    }

    private final Context context;
    private RequestQueue requestQueue;
    private Request<?> currentRequest;

    public Translation_GoogleTranslate(Context context) {
        this.context = context;
    }

    public void translateText(String textToTranslate, String targetLanguage, TranslateListener listener) {
        // Convert language codes if needed (e.g., "English" to "en")
        String targetLangCode = convertToLanguageCode(targetLanguage);
        
        // Build the URL with API key
        String url = CLOUD_TRANSLATE_URL + "?key=" + API_KEY;

        // Prepare request body
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("q", textToTranslate);
            requestBody.put("target", targetLangCode);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body", e);
            return;
        }

        // Use JsonObjectRequest instead of StringRequest
        currentRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
            response -> {
                try {
                    JSONObject data = response.getJSONObject("data");
                    JSONArray translations = data.getJSONArray("translations");
                    String translatedText = translations.getJSONObject(0).getString("translatedText");
                    listener.onSuccess(translatedText);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response", e);
                    listener.onError(new VolleyError("Error parsing response"));
                }
            },
            error -> {
                Log.e(TAG, "Translation request failed", error);
                listener.onError(error);
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        requestQueue.add(currentRequest);
    }

    public void cancelTranslation() {
        if (currentRequest != null) {
            currentRequest.cancel();
        }
    }

    private String convertToLanguageCode(String language) {
        // Convert full language names to ISO codes
        switch (language.toLowerCase()) {
            case "english":
                return "en";
            case "tagalog":
                return "tl";
            case "bisaya":
            case "cebuano":
                return "ceb";
            default:
                return language.toLowerCase();
        }
    }
}