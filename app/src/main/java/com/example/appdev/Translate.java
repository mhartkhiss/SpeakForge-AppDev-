package com.example.appdev;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class Translate {

    public interface TranslateListener {
        void onSuccess(String translatedText);
        void onError(VolleyError error);
    }

    private final Context context;

    public Translate(Context context) {
        this.context = context;
    }

    public void translateText(String textToTranslate, String targetLanguage, TranslateListener listener) {

        // Make HTTP POST request to Django translator API endpoint
        String url = Variables.translateURL;

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("text", textToTranslate);
            requestBody.put("target_language", targetLanguage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String translatedText = response.getString("translated_text");
                            listener.onSuccess(translatedText);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        listener.onError(error);
                    }
                });

        // Add the request to the RequestQueue
        RequestQueue queue = Volley.newRequestQueue(context.getApplicationContext());
        queue.add(request);
    }
}
