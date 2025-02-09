package com.example.appdev.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appdev.R;
import com.example.appdev.Variables;
import com.example.appdev.ConversationalActivity;
import com.example.appdev.models.Languages;
import com.example.appdev.utils.SpeechRecognitionDialog;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.example.appdev.models.User;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.app.Dialog;
import android.view.Window;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.models.TranslationHistory;
import com.example.appdev.utils.TranslationHistoryManager;
import com.example.appdev.adapters.TranslationHistoryAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import com.example.appdev.translators.TranslatorFactory;
import com.example.appdev.translators.TranslatorType;
import android.os.AsyncTask;
import com.example.appdev.utils.SpeechRecognitionHelper;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.LinearLayout;
import android.view.Gravity;

import com.example.appdev.utils.LoadingDotsView;

public class BasicTranslationFragment extends Fragment {

    private static final int SPEECH_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextView textViewResult;
    private Spinner outputLanguageSelection;
    private TextInputEditText textInput;
    private TextInputLayout textInputLayout;
    private FloatingActionButton btnStartSpeech;
    private ExtendedFloatingActionButton btnTranslate, btnClear;
    private View rootView;
    private Handler animationHandler;
    private Runnable animationRunnable;
    private SpeechRecognitionDialog speechDialog;
    private StringBuilder speechBuilder = new StringBuilder();
    private SpeechRecognizer speechRecognizer;
    private ImageButton btnHistory;
    private TranslationHistoryManager historyManager;
    private TextView currentLanguageLabel;
    private DatabaseReference userRef;
    private TextView currentTranslatorText;
    private ImageView translatorIcon;
    private View resultCard;
    private boolean isTranslating = false;
    private FloatingActionButton stopTranslationButton;
    private AsyncTask<String, Void, String> currentTranslator;
    private FloatingActionButton btnStartConversation;
    private SpeechRecognitionHelper speechHelper;
    private LoadingDotsView loadingDotsView;
    private TextView[] dots;
    private int currentDotIndex = 0;
    private Handler dotsHandler = new Handler();
    private Runnable dotsAnimation;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_basictranslation, container, false);
        
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (isTranslating) return;  // Skip keyboard checks during translation

                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is shown - always hide result card
                    resultCard.setVisibility(View.GONE);
                } else {
                    // Keyboard is hidden - show if we have a translation
                    if (!TextUtils.isEmpty(textViewResult.getText()) && 
                        !textViewResult.getText().toString().contains("Translating")) {
                        resultCard.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        textInput = view.findViewById(R.id.textInputEditText);
        textInputLayout = view.findViewById(R.id.textInputLayout);
        textViewResult = view.findViewById(R.id.txtTranslatedText);
        currentLanguageLabel = view.findViewById(R.id.currentLanguageLabel);
        btnStartSpeech = view.findViewById(R.id.startSpeakingButton);
        btnTranslate = view.findViewById(R.id.translateButton);
        btnClear = view.findViewById(R.id.clearButton);
        currentTranslatorText = view.findViewById(R.id.currentTranslatorText);
        translatorIcon = view.findViewById(R.id.translatorIcon);
        btnStartConversation = view.findViewById(R.id.startConversationButton);
        outputLanguageSelection = view.findViewById(R.id.languageSpinner);  // Initialize Spinner here

        // Initialize spinner with default values
        setupLanguageSpinners();  // Initial setup

        // Initialize button states
        String currentText = textInput.getText().toString().trim();
        boolean hasText = !currentText.isEmpty();
        btnStartSpeech.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnStartConversation.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnTranslate.setVisibility(hasText ? View.VISIBLE : View.GONE);
        btnClear.setVisibility(hasText ? View.VISIBLE : View.GONE);

        // Get current user's language and translator from Firebase
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User currentUser = snapshot.getValue(User.class);
                if (currentUser != null && currentUser.getLanguage() != null) {
                    Variables.userLanguage = currentUser.getLanguage();
                    currentLanguageLabel.setText(Variables.userLanguage);
                    
                    // Update spinner with filtered languages
                    if (outputLanguageSelection != null) {
                        setupLanguageSpinners();
                    }

                    // Update translator text and icon based on user's selected translator
                    TranslatorType translatorType = TranslatorType.fromId(currentUser.getTranslator());
                    currentTranslatorText.setText(translatorType.getDisplayName());
                    translatorIcon.setImageResource(translatorType.getIconResourceId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
                CustomNotification.showNotification(requireActivity(), 
                    "Failed to load user language and translator info", false);
            }
        });

        setListeners(view);

        // Initialize history manager
        historyManager = new TranslationHistoryManager(requireContext());
        
        // Initialize history button
        btnHistory = view.findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> showHistoryDialog());

        resultCard = view.findViewById(R.id.resultCard);

        // Initially hide result card
        resultCard.setVisibility(View.GONE);

        stopTranslationButton = view.findViewById(R.id.stopTranslationButton);
        stopTranslationButton.setOnClickListener(v -> stopTranslation());

        speechHelper = new SpeechRecognitionHelper(requireActivity());
    }

    private void setListeners(View view) {
        //VOICE-TO-TEXT TRANSLATION LISTENERS
        btnStartSpeech.setOnClickListener(v -> checkPermissionAndStartSpeechRecognition());

        //TEXT-TO-TEXT TRANSLATION LISTENERS
        btnTranslate.setOnClickListener(v -> {
            translateAnimation();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(textInput.getWindowToken(), 0);
            }
            String targetLanguage = outputLanguageSelection.getSelectedItem().toString();
            translateAndDisplay(textInput.getText().toString(), targetLanguage);
        });

        // Clear button listener
        btnClear.setOnClickListener(v -> {
            textInput.setText("");
            textViewResult.setText("");
            textViewResult.setTextColor(getResources().getColor(android.R.color.darker_gray));
            updateButtonVisibility(false);
            
            // Hide result card with animation
            resultCard.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> resultCard.setVisibility(View.GONE))
                    .start();
        });

        textInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonVisibility(s.toString().trim().length() > 0);
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

        btnStartConversation.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ConversationalActivity.class);
            startActivity(intent);
        });
    }

    private void updateButtonVisibility(boolean hasText) {
        btnStartSpeech.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnStartConversation.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnTranslate.setVisibility(hasText ? View.VISIBLE : View.GONE);
        btnClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
    }

    private void translateAnimation() {
        if (textViewResult == null) return;
        
        // Clear any existing text
        textViewResult.setText("");
        
        // Initialize loading dots view if not already added
        if (loadingDotsView == null) {
            loadingDotsView = new LoadingDotsView(requireContext());
            
            // Find the parent ViewGroup that contains textViewResult
            ViewGroup resultContainer = (ViewGroup) resultCard.findViewById(R.id.resultContainer);
            
            // Add the dots view to the result container
            if (resultContainer != null) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.gravity = Gravity.CENTER;
                loadingDotsView.setLayoutParams(params);
                
                resultContainer.addView(loadingDotsView);
            }
        }
        
        loadingDotsView.setVisibility(View.VISIBLE);
        textViewResult.setVisibility(View.GONE);
        
        // Start dots animation
        loadingDotsView.startAnimation();
        isTranslating = true;
    }

    private void stopAnimation() {
        if (loadingDotsView != null) {
            loadingDotsView.stopAnimation();
            loadingDotsView.setVisibility(View.GONE);
        }
    }

    private void startTranslation() {
        isTranslating = true;
        
        float disabledAlpha = 0.5f;
        
        textInput.setEnabled(false);
        textInput.animate().alpha(disabledAlpha).setDuration(300);
        
        btnTranslate.setEnabled(false);
        btnTranslate.animate().alpha(disabledAlpha).setDuration(300);
        
        btnClear.setEnabled(false);
        btnClear.animate().alpha(disabledAlpha).setDuration(300);
        
        btnStartConversation.setEnabled(false);
        btnStartConversation.animate().alpha(disabledAlpha).setDuration(300);
        
        outputLanguageSelection.setEnabled(false);
        outputLanguageSelection.animate().alpha(disabledAlpha).setDuration(300);
        
        stopTranslationButton.setVisibility(View.VISIBLE);
    }

    private void stopTranslation() {
        if (currentTranslator != null) {
            currentTranslator.cancel(true);
            currentTranslator = null;
        }

        // Reset UI
        stopAnimation();
        enableInputSection();
        
        // Hide result card with animation
        resultCard.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    resultCard.setVisibility(View.GONE);
                    textViewResult.setText("");  // Clear the translated text
                })
                .start();
            
        stopTranslationButton.setVisibility(View.GONE);
        isTranslating = false;
    }

    private void enableInputSection() {
        textInput.setEnabled(true);
        textInput.animate().alpha(1f).setDuration(300);
        
        btnTranslate.setEnabled(true);
        btnTranslate.animate().alpha(1f).setDuration(300);
        
        btnClear.setEnabled(true);
        btnClear.animate().alpha(1f).setDuration(300);
        
        btnStartConversation.setEnabled(true);
        btnStartConversation.animate().alpha(1f).setDuration(300);
        
        outputLanguageSelection.setEnabled(true);
        outputLanguageSelection.animate().alpha(1f).setDuration(300);
        
        stopTranslationButton.setVisibility(View.GONE);
    }

    private void translateAndDisplay(String text, String targetLanguage) {
        if (text.isEmpty()) return;

        if (currentTranslator != null) {
            currentTranslator.cancel(true);
        }

        // Start translation UI state
        startTranslation();

        // Show result card with animation
        resultCard.setVisibility(View.VISIBLE);
        resultCard.setAlpha(0f);
        resultCard.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }

        // Set to single translation mode (not variations)
        Variables.openAiPrompt = 1;

        // Start animation
        translateAnimation();

        currentTranslator = TranslatorFactory.createTranslator(
            TranslatorType.fromId(Variables.userTranslator),
            targetLanguage,
            translatedMessage -> {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    handleTranslationResult(translatedMessage);
                    enableInputSection();
                    isTranslating = false;
                });
            },
            requireContext()
        );
        currentTranslator.execute(text);
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

    private void startSpeechRecognition() {
        speechHelper.startSpeechRecognition(text -> {
            textInput.setText(text);
            String targetLanguage = outputLanguageSelection.getSelectedItem().toString();
            translateAndDisplay(text, targetLanguage);
        });
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
        String sourceLanguage = Variables.userLanguage;
        String translator = getCurrentTranslator();
        TranslationHistory history = new TranslationHistory(
            originalText, translatedText, sourceLanguage, targetLanguage, translator);
        historyManager.saveTranslation(history);
    }

    private String getCurrentTranslator() {
        String translatorText = currentTranslatorText.getText().toString();
        if (translatorText.contains("OpenAI")) return "openai";
        if (translatorText.contains("DeepSeek")) return "deepseek";
        if (translatorText.contains("GPT-4")) return "gpt4";
        if (translatorText.contains("Gemini")) return "gemini";
        if (translatorText.contains("Claude")) return "claude";
        return "google";
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

    private void setupLanguageSpinners() {
        if (outputLanguageSelection == null || !isAdded()) return;

        // Get all languages except the current user's language
        List<String> languages = Languages.getAllLanguages();
        List<String> outputLanguages = new ArrayList<>(languages);
        String currentLanguage = Variables.userLanguage != null ? Variables.userLanguage : "English";
        outputLanguages.remove(currentLanguage);

        // Setup output language spinner with filtered languages
        ArrayAdapter<String> outputAdapter = new ArrayAdapter<>(requireContext(),
            R.layout.simple_spinner_item_custom, outputLanguages);
        outputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        outputLanguageSelection.setAdapter(outputAdapter);

        // Update current language label
        if (currentLanguageLabel != null) {
            currentLanguageLabel.setText(currentLanguage);
        }
    }

    // Add cleanup in onDestroy to prevent memory leaks
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTranslation();
        stopAnimation();
        if (speechHelper != null) {
            speechHelper.destroy();
        }
        dotsHandler.removeCallbacksAndMessages(null);
    }

    // Helper methods to handle translation results
    private void handleTranslationResult(String translatedText) {
        if (!TextUtils.isEmpty(translatedText)) {
            stopAnimation();
            textViewResult.setVisibility(View.VISIBLE);
            textViewResult.setText(translatedText);
            textViewResult.setTextColor(getResources().getColor(R.color.black));
            textViewResult.setTextSize(38);
            textViewResult.setAlpha(1.0f);
            
            if (loadingDotsView != null) {
                loadingDotsView.setVisibility(View.GONE);
            }
        }
        saveToHistory(textInput.getText().toString(), translatedText, 
            outputLanguageSelection.getSelectedItem().toString());
    }

    private void handleTranslationError() {
        stopAnimation();
        textViewResult.setText("Translation failed");
        textViewResult.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        textViewResult.setTextSize(38);
        textViewResult.setAlpha(1.0f);
    }

}