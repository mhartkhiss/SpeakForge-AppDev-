package com.example.appdev.classes;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.appdev.R;
import com.example.appdev.classes.Variables;
import com.example.appdev.models.LanguageModel;
import com.google.firebase.database.DatabaseError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class FetchLanguages {

    public static void fetchSupportedLanguages(Context context, final LanguagesListener listener) {
        String url = Variables.supportedLanguagesURL;
        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Parse the response
                        ArrayList<LanguageModel> languages = new ArrayList<>();
                        try {
                            // Parse the response here
                            JSONArray jsonArray = response.getJSONArray("languages");

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject languageObject = jsonArray.getJSONObject(i);
                                String name = languageObject.getString("name");
                                String code = languageObject.getString("code");
                                languages.add(new LanguageModel(name, code));
                            }

                            // Notify listener with supported languages
                            if (listener != null) {
                                listener.onLanguagesReceived(languages);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        if (listener != null) {
                            listener.onError(error);
                        }
                    }
                });

        queue.add(request);
    }

    public interface LanguagesListener {
        void onLanguagesReceived(ArrayList<LanguageModel> languages);
        void onError(VolleyError error);
    }


    /*private static void updateLanguagesSpinner(Context context, ArrayList<String> languageNames) {

        // Find the spinner directly from the context
        Spinner spinner = ((Activity) context).findViewById(R.id.languageSpinner);
        Spinner spinner2 = ((Activity) context).findViewById(R.id.languageSpinner2);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, languageNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner2.setAdapter(adapter);

    }*/

}

