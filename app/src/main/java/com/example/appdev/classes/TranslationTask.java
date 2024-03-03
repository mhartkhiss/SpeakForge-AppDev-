package com.example.appdev.classes;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TranslationTask extends AsyncTask<String, Void, String> {

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
            String requestBody = "input_text=" + URLEncoder.encode(inputText, "UTF-8");

            // Write the request body to the connection
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(requestBody.getBytes());
            outputStream.flush();
            outputStream.close();

            // Read the response from the server
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse the JSON response
            translatedText = response.toString();

            // Close the connection
            connection.disconnect();
        } catch (IOException e) {
            Log.e("TranslationTask", "Error: " + e.getMessage());
        }

        return translatedText;
    }

    @Override
    protected void onPostExecute(String translatedText) {
        // Here you can update your UI with the translated text
        // For example, if you have a TextView named textViewResult:
        // textViewResult.setText(translatedText);
    }
}

