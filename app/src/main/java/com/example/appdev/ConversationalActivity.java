package com.example.appdev;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import android.widget.AdapterView;
import android.os.AsyncTask;
import android.os.Build;

import com.example.appdev.models.Languages;
import com.example.appdev.utils.SpeechRecognitionDialog;
import com.example.appdev.utils.SpeechRecognitionHelper;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.appdev.translators.TranslatorFactory;
import com.example.appdev.translators.TranslatorType;

public class ConversationalActivity extends AppCompatActivity {
    
    // User 1 (Bottom) Views
    private TextView user1Result;
    private Spinner user1LanguageSpinner;
    private FloatingActionButton user1SpeakButton;
    
    // User 2 (Top) Views
    private TextView user2Result;
    private Spinner user2LanguageSpinner;
    private FloatingActionButton user2SpeakButton;

    // Speech Recognition
    private SpeechRecognitionHelper speechHelper;
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversational);

        // Initialize User 1 (Bottom) Views
        user1Result = findViewById(R.id.user1Result);
        user1LanguageSpinner = findViewById(R.id.user1LanguageSpinner);
        user1SpeakButton = findViewById(R.id.user1SpeakButton);

        // Initialize User 2 (Top) Views
        user2Result = findViewById(R.id.user2Result);
        user2LanguageSpinner = findViewById(R.id.user2LanguageSpinner);
        user2SpeakButton = findViewById(R.id.user2SpeakButton);

        // Initialize Speech Recognition Helper
        speechHelper = new SpeechRecognitionHelper(this);

        // Setup language spinners
        setupLanguageSpinners();

        // Setup click listeners
        setupClickListeners();
    }

    private void setupLanguageSpinners() {
        // Setup User 1 Spinner
        ArrayAdapter<String> user1Adapter = new ArrayAdapter<>(this,
            R.layout.simple_spinner_item_custom, Languages.getUser1Languages());
        user1Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        user1LanguageSpinner.setAdapter(user1Adapter);
        
        // Set default selection for User 1
        int user1Position = user1Adapter.getPosition(Languages.getUser1Language());
        if (user1Position >= 0) {
            user1LanguageSpinner.setSelection(user1Position);
        }

        // Setup User 2 Spinner
        ArrayAdapter<String> user2Adapter = new ArrayAdapter<>(this,
            R.layout.simple_spinner_item_custom, Languages.getUser2Languages());
        user2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        user2LanguageSpinner.setAdapter(user2Adapter);
        
        // Set default selection for User 2
        int user2Position = user2Adapter.getPosition(Languages.getUser2Language());
        if (user2Position >= 0) {
            user2LanguageSpinner.setSelection(user2Position);
        }

        // Add listeners to update available languages when selections change
        user1LanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                Languages.setUser1Language(selectedLanguage);
                
                // Update User 2's spinner
                user2Adapter.clear();
                user2Adapter.addAll(Languages.getUser2Languages());
                user2Adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        user2LanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                Languages.setUser2Language(selectedLanguage);
                
                // Update User 1's spinner
                user1Adapter.clear();
                user1Adapter.addAll(Languages.getUser1Languages());
                user1Adapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        user1SpeakButton.setOnClickListener(v -> {
            checkPermissionAndStartSpeechRecognition(true);
        });

        user2SpeakButton.setOnClickListener(v -> {
            checkPermissionAndStartSpeechRecognition(false);
        });
    }

    private void checkPermissionAndStartSpeechRecognition(boolean isUser1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionResult = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO);
            if (permissionResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition(isUser1);
            } else {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            }
        } else {
            startSpeechRecognition(isUser1);
        }
    }

    private void startSpeechRecognition(boolean isUser1) {
        // Create a custom dialog for User 2 that's rotated 180 degrees
        SpeechRecognitionDialog dialog = new SpeechRecognitionDialog(this, 
            new SpeechRecognitionDialog.SpeechRecognitionListener() {
                @Override
                public void onCancelled() {
                    speechHelper.stopListening();
                }

                @Override
                public void onFinished(String text) {
                    speechHelper.stopListening();
                    if (!text.isEmpty()) {
                        String targetLanguage;
                        if (isUser1) {
                            targetLanguage = user2LanguageSpinner.getSelectedItem().toString();
                        } else {
                            targetLanguage = user1LanguageSpinner.getSelectedItem().toString();
                        }
                        translateAndDisplay(text, targetLanguage, isUser1);
                    }
                }
            }, !isUser1);  // Pass isUpsideDown parameter

        speechHelper.startSpeechRecognition(text -> {
            if (!text.isEmpty()) {
                String targetLanguage;
                if (isUser1) {
                    targetLanguage = user2LanguageSpinner.getSelectedItem().toString();
                } else {
                    targetLanguage = user1LanguageSpinner.getSelectedItem().toString();
                }
                translateAndDisplay(text, targetLanguage, isUser1);
            }
        });
    }

    private void startTranslation(boolean isUser1) {
        float disabledAlpha = 0.5f;
        
        // Disable and fade the active user's controls
        if (isUser1) {
            user1SpeakButton.setEnabled(false);
            user1SpeakButton.animate().alpha(disabledAlpha).setDuration(300);
            user1LanguageSpinner.setEnabled(false);
            user1LanguageSpinner.animate().alpha(disabledAlpha).setDuration(300);
        } else {
            user2SpeakButton.setEnabled(false);
            user2SpeakButton.animate().alpha(disabledAlpha).setDuration(300);
            user2LanguageSpinner.setEnabled(false);
            user2LanguageSpinner.animate().alpha(disabledAlpha).setDuration(300);
        }
    }

    private void enableControls(boolean isUser1) {
        // Re-enable and restore opacity of the active user's controls
        if (isUser1) {
            user1SpeakButton.setEnabled(true);
            user1SpeakButton.animate().alpha(1f).setDuration(300);
            user1LanguageSpinner.setEnabled(true);
            user1LanguageSpinner.animate().alpha(1f).setDuration(300);
        } else {
            user2SpeakButton.setEnabled(true);
            user2SpeakButton.animate().alpha(1f).setDuration(300);
            user2LanguageSpinner.setEnabled(true);
            user2LanguageSpinner.animate().alpha(1f).setDuration(300);
        }
    }

    private void translateAndDisplay(String text, String targetLanguage, boolean isUser1) {
        // Start translation UI state
        startTranslation(isUser1);

        // Create and execute translator
        AsyncTask<String, Void, String> translator = TranslatorFactory.createTranslator(
            TranslatorType.fromId(Variables.userTranslator),
            targetLanguage,
            translatedMessage -> {
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        if (isUser1) {
                            user2Result.setText(translatedMessage);
                        } else {
                            user1Result.setText(translatedMessage);
                        }
                        enableControls(isUser1);
                    });
                }
            },
            this
        );
        translator.execute(text);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Since we don't know which user triggered the permission request,
                // we'll show a notification to try again
                CustomNotification.showNotification(this, 
                    "Permission granted! Please try speaking again.", true);
            } else {
                CustomNotification.showNotification(this, 
                    "Microphone permission is required for speech recognition", false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
    }
} 