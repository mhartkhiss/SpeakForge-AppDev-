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

public class SpeechRecognitionHelper {
    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private SpeechRecognitionDialog speechDialog;
    private final StringBuilder speechBuilder = new StringBuilder();
    private SpeechRecognitionCallback callback;

    public interface SpeechRecognitionCallback {
        void onSpeechResult(String text);
    }

    public SpeechRecognitionHelper(Activity activity) {
        this.activity = activity;
    }

    public void startSpeechRecognition(SpeechRecognitionCallback callback) {
        this.callback = callback;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

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
        });

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

            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            CustomNotification.showNotification(activity, "Speech recognition not available", false);
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
                speechRecognizer = null;
            } catch (Exception ignored) {}
        }
        if (speechDialog != null && speechDialog.isShowing()) {
            speechDialog.dismiss();
        }
    }

    public void destroy() {
        stopListening();
    }
} 