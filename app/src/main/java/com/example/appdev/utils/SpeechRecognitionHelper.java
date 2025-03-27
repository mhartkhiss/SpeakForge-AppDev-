package com.example.appdev.utils;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Locale;
import com.example.appdev.Variables;
import com.example.appdev.models.Languages;

public class SpeechRecognitionHelper {
    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private SpeechRecognitionDialog speechDialog;
    private final StringBuilder speechBuilder = new StringBuilder();
    private SpeechRecognitionCallback callback;
    private boolean isUpsideDown = false;
    private Intent recognizerIntent;
    private OnSpeechResultListener resultListener;
    private boolean isListening = false;

    public interface SpeechRecognitionCallback {
        void onSpeechResult(String text);
    }

    public interface OnSpeechResultListener {
        void onResult(String text);
    }

    public SpeechRecognitionHelper(Activity activity) {
        this.activity = activity;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
        setupRecognizerIntent();
        setupRecognitionListener();
    }

    private Locale getLocaleFromLanguage(String language) {
        return Languages.getLocaleForLanguage(language);
    }

    private void setupRecognizerIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        // Add these flags for continuous recognition
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 150000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 150000);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000);
    }

    private void setupRecognitionListener() {
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                speechDialog.show();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {
                if (speechDialog != null) {
                    speechDialog.updateVoiceAnimation(rmsdB);
                }
            }

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                // Restart listening if we're still in listening mode
                if (isListening) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }

            @Override
            public void onError(int error) {
                // Restart listening on recoverable errors if we're still in listening mode
                if (isListening && (error == SpeechRecognizer.ERROR_NO_MATCH || 
                                  error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }

            @Override
            public void onResults(Bundle results) {
                if (resultListener != null) {
                    String text = getRecognizedText(results);
                    if (!text.isEmpty()) {
                        resultListener.onResult(text);
                    }
                }
                // Restart listening if we're still in listening mode
                if (isListening) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (resultListener != null) {
                    String text = getRecognizedText(partialResults);
                    if (!text.isEmpty()) {
                        resultListener.onResult(text);
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    private String getRecognizedText(Bundle results) {
        if (results != null) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                return matches.get(0);
            }
        }
        return "";
    }

    public void startSpeechRecognition(SpeechRecognitionCallback callback, boolean isUpsideDown) {
        this.callback = callback;
        this.isUpsideDown = isUpsideDown;
        
        // Get the user's selected language
        String userLanguage = Variables.userLanguage != null ? Variables.userLanguage : "English";
        Locale locale = getLocaleFromLanguage(userLanguage);
        
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);

        // Check if the language is supported on the device
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            CustomNotification.showNotification(activity, 
                "Speech recognition is not available on this device", false);
            return;
        }

        speechBuilder.setLength(0);
        
        speechDialog = new SpeechRecognitionDialog(activity, new SpeechRecognitionDialog.SpeechRecognitionListener() {
            @Override
            public void onCancelled() {
                stopListening();
            }

            @Override
            public void onFinished(String text) {
                stopListening();
                if (!text.isEmpty() && callback != null) {
                    callback.onSpeechResult(text);
                }
            }
        }, isUpsideDown);

        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    speechDialog.show();
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {
                    if (speechDialog != null) {
                        speechDialog.updateVoiceAnimation(rmsdB);
                    }
                }

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    String errorMessage;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            errorMessage = "Audio recording error";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            errorMessage = "Client side error";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            errorMessage = "Insufficient permissions";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            errorMessage = "Network error";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            errorMessage = "Network timeout";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            errorMessage = "No speech input";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            errorMessage = "Recognition service busy";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            errorMessage = "Server error";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            errorMessage = "No speech input";
                            break;
                        default:
                            errorMessage = "Speech recognition error";
                            break;
                    }
                    if (speechDialog != null && speechDialog.isShowing()) {
                        speechDialog.dismiss();
                    }
                    CustomNotification.showNotification(activity, errorMessage, false);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        speechBuilder.append(text);
                        speechDialog.updateRecognizedText(speechBuilder.toString());
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0);
                        speechDialog.updateRecognizedText(text);
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            speechRecognizer.startListening(recognizerIntent);
        } catch (Exception e) {
            CustomNotification.showNotification(activity, "Speech recognition not available", false);
        }
    }

    public void startSpeechRecognition(SpeechRecognitionCallback callback) {
        startSpeechRecognition(callback, false);
    }

    public void startListening(OnSpeechResultListener listener) {
        this.resultListener = listener;
        isListening = true;
        speechRecognizer.startListening(recognizerIntent);
    }

    public void stopListening() {
        isListening = false;
        speechRecognizer.stopListening();
        if (speechDialog != null && speechDialog.isShowing()) {
            speechDialog.dismiss();
        }
    }

    public void destroy() {
        isListening = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    public void setLanguage(String language) {
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
    }
} 