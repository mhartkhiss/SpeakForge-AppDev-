package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class TestTranslate extends AppCompatActivity {

    private TextView editText;
    private TextView translatedTextView;
    private Button translateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_translate);

        editText = findViewById(R.id.editText);
        translatedTextView = findViewById(R.id.translatedTextView);
        translateButton = findViewById(R.id.translateButton);

        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                translateText();
            }
        });
    }

    private void translateText() {
        String textToTranslate = "Hello can i ask where is the subway station?";
        String targetLanguage = "filipino";

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
                            translatedTextView.setText(translatedText);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        error.printStackTrace();
                    }
                });

        // Add the request to the RequestQueue
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        queue.add(request);
    }
}
