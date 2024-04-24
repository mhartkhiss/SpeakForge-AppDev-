package com.example.appdev.classes;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TranslationTask_OpenAI extends AsyncTask<String, Void, String> {

    private String targetLanguage;
    private TranslationListener listener;

    public TranslationTask_OpenAI(String targetLanguage, TranslationListener listener) {
        this.targetLanguage = targetLanguage;
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        String inputText = strings[0];
        String translatedText = "";

        try {
            // Set up the URL for the translation endpoint
            URL url = new URL(Variables.translateURL2);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            // Construct the request body
            String requestBody = "input_text=" + URLEncoder.encode(inputText, "UTF-8")
                    + "&target_language=" + URLEncoder.encode(targetLanguage, "UTF-8");

            // Write the request body to the connection
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(requestBody.getBytes());
            outputStream.flush();
            outputStream.close();

            // Check the HTTP response code
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the JSON response to extract the translated text
                JSONObject jsonResponse = new JSONObject(response.toString());
                translatedText = jsonResponse.optString("translated_text");
            } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                // Retry the task
                Log.e("TranslationTask", "HTTP 500 error, retrying task...");
                return doInBackground(strings); // Retry the task recursively
            }

            // Close the connection
            connection.disconnect();
        } catch (IOException | JSONException e) {
            Log.e("TranslationTask", "Error: " + e.getMessage());
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
