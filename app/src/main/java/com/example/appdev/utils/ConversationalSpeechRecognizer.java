package com.example.appdev.utils;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Locale;
import com.example.appdev.models.Languages;

public class ConversationalSpeechRecognizer {
    private final Activity activity;
    private SpeechRecognizer speechRecognizer;
    private OnSpeechResultListener resultListener;
    private String selectedLanguage;
    private boolean isListening = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRecognitionInProgress = false;
    private final StringBuilder accumulatedText = new StringBuilder();
    private String lastPartialResult = "";

    public interface OnSpeechResultListener {
        void onPartialResult(String text);
        void onError(String errorMessage);
    }

    public ConversationalSpeechRecognizer(Activity activity) {
        this.activity = activity;
    }

    public void startListening(String language, OnSpeechResultListener listener) {
        this.resultListener = listener;
        this.selectedLanguage = language;
        this.isListening = true;
        this.isRecognitionInProgress = false;
        this.accumulatedText.setLength(0); // Clear accumulated text when starting new session
        this.lastPartialResult = "";
        
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity);
            speechRecognizer.setRecognitionListener(createRecognitionListener());
            startRecognition();
        } catch (Exception e) {
            if (resultListener != null) {
                resultListener.onError("Speech recognition initialization failed");
            }
        }
    }

    private void startRecognition() {
        if (!isListening || isRecognitionInProgress) return;

        handler.postDelayed(() -> {
            if (!isListening) return;
            
            try {
                isRecognitionInProgress = true;
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                
                // Use the selected language's locale
                Locale locale = Languages.getLocaleForLanguage(selectedLanguage);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString());
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toString());
                intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
                intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

                speechRecognizer.startListening(intent);
            } catch (Exception e) {
                isRecognitionInProgress = false;
                if (resultListener != null) {
                    resultListener.onError("Failed to start speech recognition");
                }
            }
        }, 100); // Small delay before starting next recognition
    }

    private RecognitionListener createRecognitionListener() {
        return new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                isRecognitionInProgress = false;
                // Restart listening if still active
                if (isListening) {
                    startRecognition();
                }
            }

            @Override
            public void onError(int error) {
                isRecognitionInProgress = false;
                
                if (!isListening) return; // Don't process errors if we're not supposed to be listening

                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        // Silently restart on these errors
                        startRecognition();
                        return;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        // If busy, wait a bit longer and try again
                        handler.postDelayed(() -> startRecognition(), 500);
                        return;
                    case SpeechRecognizer.ERROR_AUDIO:
                        notifyError("Audio recording error");
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        notifyError("Network error");
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        notifyError("Network timeout");
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        // Restart with a longer delay on client error
                        handler.postDelayed(() -> startRecognition(), 800);
                        return;
                    case SpeechRecognizer.ERROR_SERVER:
                        notifyError("Server error");
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        notifyError("Insufficient permissions");
                        break;
                    default:
                        notifyError("Speech recognition error");
                        break;
                }
            }

            private void notifyError(String message) {
                if (resultListener != null) {
                    resultListener.onError(message);
                }
            }

            @Override
            public void onResults(Bundle results) {
                isRecognitionInProgress = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && resultListener != null) {
                    String recognizedText = matches.get(0);
                    
                    // Only append if it's different from the last partial result
                    if (!recognizedText.equals(lastPartialResult)) {
                        // Add space if needed
                        if (accumulatedText.length() > 0) {
                            accumulatedText.append(" ");
                        }
                        accumulatedText.append(recognizedText);
                        resultListener.onPartialResult(accumulatedText.toString());
                    }
                }
                // Continue listening if still active
                if (isListening) {
                    startRecognition();
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty() && resultListener != null) {
                    lastPartialResult = matches.get(0);
                    // Show partial results without accumulating
                    String fullText = accumulatedText.length() > 0 ? 
                        accumulatedText + " " + lastPartialResult : 
                        lastPartialResult;
                    resultListener.onPartialResult(fullText);
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {}
        };
    }

    public void stopListening() {
        isListening = false;
        isRecognitionInProgress = false;
        handler.removeCallbacksAndMessages(null);
        accumulatedText.setLength(0);
        lastPartialResult = "";
        
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }
    }

    public void destroy() {
        stopListening();
    }
} 