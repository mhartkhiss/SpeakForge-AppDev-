package com.example.appdev.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.VolleyError;
import com.example.appdev.R;
import com.example.appdev.Variables;
import com.example.appdev.translators.Translation_GoogleTranslate;
import com.example.appdev.translators.Translation_OpenAI;
import com.example.appdev.models.Languages;
import com.example.appdev.utils.SpeechRecognitionDialog;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.app.Dialog;
import android.view.Window;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.models.TranslationHistory;
import com.example.appdev.utils.TranslationHistoryManager;
import com.example.appdev.adapters.TranslationHistoryAdapter;

public class BasicTranslationFragment extends Fragment {

    private static final int SPEECH_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextView textViewResult;
    private Spinner outputLanguageSelection;
    private TextInputEditText textInput;
    private TextInputLayout textInputLayout;
    private Button btnStartSpeech, btnTranslate;
    private View rootView;
    private ProgressBar progressBar;
    private Handler animationHandler;
    private Runnable animationRunnable;
    private SpeechRecognitionDialog speechDialog;
    private StringBuilder speechBuilder = new StringBuilder();
    private SpeechRecognizer speechRecognizer;
    private ImageButton btnHistory;
    private TranslationHistoryManager historyManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // HIDING THE TEXTVIEW WHEN THE KEYBOARD IS OPEN
        rootView = inflater.inflate(R.layout.fragment_basictranslation, container, false);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();

                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    textViewResult.setVisibility(View.GONE);
                } else {
                    textViewResult.setVisibility(View.VISIBLE);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        textInput = view.findViewById(R.id.textInputEditText);
        textInputLayout = view.findViewById(R.id.textInputLayout);
        textViewResult = view.findViewById(R.id.txtTranslatedText);
        progressBar = view.findViewById(R.id.translationProgress);


        outputLanguageSelection = requireView().findViewById(R.id.languageSpinner);
        String[] languages = Languages.getLanguages().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outputLanguageSelection.setAdapter(adapter);

        setListeners(view);

        // Initialize history manager
        historyManager = new TranslationHistoryManager(requireContext());
        
        // Initialize history button
        btnHistory = view.findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> showHistoryDialog());
    }

    private void setListeners(View view) {

        //VOICE-TO-TEXT TRANSLATION LISTENERS
        btnStartSpeech = view.findViewById(R.id.startSpeakingButton);
        btnStartSpeech.setOnClickListener(v -> checkPermissionAndStartSpeechRecognition());

        //TEXT-TO-TEXT TRANSLATION LISTENERS
        textInput = view.findViewById(R.id.textInputEditText);
        btnTranslate = view.findViewById(R.id.translateButton);
        btnTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                translateAnimation();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
                }
                String targetLanguage = BasicTranslationFragment.this.outputLanguageSelection.getSelectedItem().toString();
                translateAndDisplay(textInput.getText().toString(), targetLanguage);
            }
        });

        textInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length() > 0) {
                    btnStartSpeech.setVisibility(View.GONE);
                    btnTranslate.setVisibility(View.VISIBLE);
                } else {
                    btnStartSpeech.setVisibility(View.VISIBLE);
                    btnTranslate.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        textInput.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                String targetLanguage = this.outputLanguageSelection.getSelectedItem().toString();
                translateAndDisplay(textInput.getText().toString(), targetLanguage);

                return true;
            }

            return false;
        });
    }

    private void translateAnimation() {
        progressBar.setVisibility(View.VISIBLE);
        textViewResult.setText("Translating");
        textViewResult.setTextColor(getResources().getColor(R.color.grey));
        
        // Stop any existing animation first
        stopAnimation();
        
        // Create a StringBuilder for the dots
        StringBuilder dots = new StringBuilder();
        animationHandler = new Handler();
        animationRunnable = new Runnable() {
            private int dotCount = 0;

            @Override
            public void run() {
                // Clear previous dots and add new ones
                dots.setLength(0);
                for (int i = 0; i < 3; i++) {
                    dots.append(i < dotCount ? "." : " ");
                }
                
                // Update text with current dots
                if (textViewResult != null) {
                    textViewResult.setText("Translating" + dots.toString());
                }
                
                // Increment dot count or reset to 0 if we reached 3
                dotCount = (dotCount + 1) % 4;
                
                // Schedule next animation frame
                if (animationHandler != null) {
                    animationHandler.postDelayed(this, 300);
                }
            }
        };
        
        animationHandler.post(animationRunnable);
    }

    private void stopAnimation() {
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
            animationHandler = null;
            animationRunnable = null;
        }
    }

    private void translateAndDisplay(String text, String targetLanguage) {
        // Start animation
        translateAnimation();

        if (Variables.userTranslator.equals("openai")) {
            // OpenAI translation
            Variables.openAiPrompt = 1;
            Translation_OpenAI translationOpenAITask = new Translation_OpenAI(targetLanguage, translatedMessage -> {
                if (getActivity() == null) return; // Check if fragment is still attached
                
                requireActivity().runOnUiThread(() -> {
                    if (!TextUtils.isEmpty(translatedMessage)) {
                        String[] lines = translatedMessage.split("\n");
                        if (lines.length > 0) {
                            stopAnimation(); // Stop the animation before showing result
                            String firstLine = lines[0].replaceAll("\\d+\\.", "").trim();
                            textViewResult.setText(firstLine);
                            textViewResult.setTextColor(getResources().getColor(R.color.black));
                            textViewResult.setTextSize(38);
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                    saveToHistory(text, translatedMessage, targetLanguage);
                });
            });
            translationOpenAITask.execute(text);
        } else {
            // Google Translate
            Translation_GoogleTranslate translationGoogleTask = new Translation_GoogleTranslate(requireContext());
            translationGoogleTask.translateText(text, targetLanguage, new Translation_GoogleTranslate.TranslateListener() {
                @Override
                public void onSuccess(String translatedText) {
                    if (getActivity() == null) return; // Check if fragment is still attached
                    
                    requireActivity().runOnUiThread(() -> {
                        if (!TextUtils.isEmpty(translatedText)) {
                            stopAnimation(); // Stop the animation before showing result
                            textViewResult.setText(translatedText);
                            textViewResult.setTextColor(getResources().getColor(R.color.black));
                            textViewResult.setTextSize(38);
                        }
                        progressBar.setVisibility(View.GONE);
                        saveToHistory(text, translatedText, targetLanguage);
                    });
                }

                @Override
                public void onError(VolleyError error) {
                    if (getActivity() == null) return; // Check if fragment is still attached
                    
                    requireActivity().runOnUiThread(() -> {
                        stopAnimation(); // Stop animation on error
                        textViewResult.setText("Translation failed");
                        textViewResult.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                        progressBar.setVisibility(View.GONE);
                    });
                }
            });
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        speechBuilder.setLength(0);
        
        speechDialog = new SpeechRecognitionDialog(requireContext(), new SpeechRecognitionDialog.SpeechRecognitionListener() {
            @Override
            public void onCancelled() {
                stopListening();
            }

            @Override
            public void onFinished(String text) {
                stopListening();
                if (!text.isEmpty()) {
                    textInput.setText(text);
                    String targetLanguage = outputLanguageSelection.getSelectedItem().toString();
                    translateAndDisplay(text, targetLanguage);
                }
            }
        });
        
        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext());
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
                    CustomNotification.showNotification(requireActivity(), errorMessage, false);
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
            CustomNotification.showNotification(requireActivity(), 
                "Speech recognition not available", false);
        }
    }

    private void stopListening() {
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

    private void checkPermissionAndStartSpeechRecognition() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionResult = requireActivity().checkSelfPermission(android.Manifest.permission.RECORD_AUDIO);
            if (permissionResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            }
        } else {
            startSpeechRecognition();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startSpeechRecognition();
            } else {
                CustomNotification.showNotification(requireActivity(), 
                    "Microphone permission is required for speech recognition", false);
            }
        }
    }

    private void saveToHistory(String originalText, String translatedText, String targetLanguage) {
        TranslationHistory history = new TranslationHistory(originalText, translatedText, targetLanguage);
        historyManager.saveTranslation(history);
    }

    private void showHistoryDialog() {
        Dialog historyDialog = new Dialog(requireContext());
        historyDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        historyDialog.setContentView(R.layout.translation_history_dialog);

        RecyclerView recyclerView = historyDialog.findViewById(R.id.historyRecyclerView);
        TextView emptyText = historyDialog.findViewById(R.id.emptyHistoryText);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Add clear history button
        ImageButton btnClear = historyDialog.findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> showClearHistoryConfirmation(historyDialog));

        historyDialog.findViewById(R.id.btnClose).setOnClickListener(v -> historyDialog.dismiss());

        // Load history from local storage
        List<TranslationHistory> historyList = historyManager.getHistory();
        
        // Show/hide empty state
        if (historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }

        TranslationHistoryAdapter adapter = new TranslationHistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        Window window = historyDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        historyDialog.show();
    }

    private void showClearHistoryConfirmation(Dialog historyDialog) {
        Dialog confirmDialog = new Dialog(requireContext());
        confirmDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        confirmDialog.setContentView(R.layout.clear_history_confirmation_dialog);

        confirmDialog.findViewById(R.id.btnCancel).setOnClickListener(v -> confirmDialog.dismiss());
        
        confirmDialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            historyManager.clearHistory();
            historyDialog.dismiss();
            confirmDialog.dismiss();
            CustomNotification.showNotification(requireActivity(), 
                "History cleared", true);
        });

        Window window = confirmDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        confirmDialog.show();
    }

    // Add cleanup in onDestroy to prevent memory leaks
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAnimation();
        stopListening();
    }

}