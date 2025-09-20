package com.example.appdev.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appdev.R;
import com.example.appdev.QRScanActivity;
import com.example.appdev.Variables;
import com.example.appdev.ConversationalActivity;
import com.example.appdev.MainActivity;
import com.example.appdev.models.Languages;
import com.example.appdev.utils.SpeechRecognitionDialog;
import com.example.appdev.utils.CustomNotification;
import com.example.appdev.utils.TranslationModeManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.bumptech.glide.Glide;
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

import com.example.appdev.translators.TranslatorFactory;
import com.example.appdev.translators.TranslatorType;
import android.os.AsyncTask;
import com.example.appdev.utils.SpeechRecognitionHelper;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.LinearLayout;
import android.view.Gravity;

import com.example.appdev.utils.LoadingDotsView;
import com.example.appdev.subcontrollers.ChangeTranslatorControl;

import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;

import pl.droidsonroids.gif.GifImageView;

import android.graphics.drawable.GradientDrawable;

public class BasicTranslationFragment extends Fragment {

    private static final int SPEECH_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private TextView textViewResult;
    private Spinner outputLanguageSelection;
    private TextInputEditText textInput;
    private TextInputLayout textInputLayout;
    private FloatingActionButton btnStartSpeech;
    private MaterialButton btnTranslate, btnClear;
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
    private ValueEventListener userValueEventListener;
    private TextView currentTranslatorText;
    private ImageView translatorIcon;
    private View resultCard;
    private boolean isTranslating = false;
    private FloatingActionButton stopTranslationButton;
    private AsyncTask<String, Void, String> currentTranslator;
    private FloatingActionButton btnStartConversation;
    private de.hdodenhof.circleimageview.CircleImageView profileButton;
    private SpeechRecognitionHelper speechHelper;
    private LoadingDotsView loadingDotsView;
    private TextView[] dots;
    private int currentDotIndex = 0;
    private Handler dotsHandler = new Handler();
    private Runnable dotsAnimation;
    private View translatorSection;
    private ChangeTranslatorControl translatorControl;
    private GifImageView translatingAnimation;
    private TextView modeFeedbackText;
    private Handler feedbackHandler = new Handler();

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
        profileButton = view.findViewById(R.id.profileButton);
        outputLanguageSelection = view.findViewById(R.id.languageSpinner);  // Initialize Spinner here
        btnHistory = view.findViewById(R.id.btnHistory);
        modeFeedbackText = view.findViewById(R.id.modeFeedbackText);

        // Initialize translation mode from SharedPreferences
        TranslationModeManager.initializeFromPreferences(requireContext());
        
        // Explicitly initialize toggle button
        // Load user profile picture
        loadUserProfilePicture();

        // Initialize spinner with default values
        setupLanguageSpinners();  // Initial setup

        // Initialize button states
        String currentText = textInput.getText().toString().trim();
        boolean hasText = !currentText.isEmpty();
        btnStartSpeech.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnStartConversation.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnHistory.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnTranslate.setVisibility(hasText ? View.VISIBLE : View.GONE);
        btnClear.setVisibility(hasText ? View.VISIBLE : View.GONE);

        // Check if this is a guest user
        if ("guest".equals(Variables.userUID)) {
            // For guest users, use the values already set in Variables
            currentLanguageLabel.setText(Variables.userLanguage);
            setupLanguageSpinners();
            TranslatorType translatorType = TranslatorType.fromId(Variables.userTranslator);
            currentTranslatorText.setText(translatorType.getDisplayName());
            translatorIcon.setImageResource(translatorType.getIconResourceId());
            
            // Add click listener to allow guest users to change their source language
            View languageCard = view.findViewById(R.id.languageCard);
            if (languageCard != null) {
                languageCard.setOnClickListener(v -> showLanguageSelectionDialog());
            } else {
                // If languageCard is not found, add click listener directly to the label
                currentLanguageLabel.setOnClickListener(v -> showLanguageSelectionDialog());
            }
            
            // Disable translator selection for guest users
            View translatorSection = view.findViewById(R.id.translatorSection);
            if (translatorSection != null) {
                translatorSection.setClickable(false);
                translatorSection.setFocusable(false);
                // Add visual indication that it's disabled
                translatorSection.setAlpha(0.7f);
            }
        }
        // Get current user's language and translator from Firebase for regular users
        else if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            
            userValueEventListener = new ValueEventListener() {
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
                    // Only show notification if we're still attached to an activity
                    // AND the user is still logged in (not just logging out)
                    if (isAdded() && getActivity() != null && 
                        FirebaseAuth.getInstance().getCurrentUser() != null) {
                        CustomNotification.showNotification(requireActivity(), 
                            "Failed to load user language and translator info", false);
                    }
                }
            };
            userRef.addValueEventListener(userValueEventListener);
        } else {
            // Set default values if user is not authenticated
            Variables.userLanguage = "English";
            currentLanguageLabel.setText(Variables.userLanguage);
            setupLanguageSpinners();
            TranslatorType translatorType = TranslatorType.fromId("google");
            currentTranslatorText.setText(translatorType.getDisplayName());
            translatorIcon.setImageResource(translatorType.getIconResourceId());
        }

        setListeners(view);

        // Initialize history manager
        historyManager = new TranslationHistoryManager(requireContext());
        
        // Initialize history button click listener
        btnHistory.setOnClickListener(v -> showHistoryDialog());

        resultCard = view.findViewById(R.id.resultCard);

        // Initially hide result card
        resultCard.setVisibility(View.GONE);

        stopTranslationButton = view.findViewById(R.id.stopTranslationButton);
        stopTranslationButton.setOnClickListener(v -> stopTranslation());

        speechHelper = new SpeechRecognitionHelper(requireActivity());

        // Initialize translator control
        translatorControl = new ChangeTranslatorControl(this);

        // Initialize translator section and set click listener
        translatorSection = view.findViewById(R.id.translatorSection);
        // Only allow regular users to change the translator
        if (!"guest".equals(Variables.userUID)) {
            translatorSection.setOnClickListener(v -> showTranslatorSelectionDialog(v));
        }

        translatingAnimation = view.findViewById(R.id.translatingAnimation);
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
            showConversationalModeSelectionDialog();
        });

        // Add formality toggle listener
        if (profileButton != null) {
            profileButton.setOnClickListener(v -> openProfile());
        }
    }

    private void updateButtonVisibility(boolean hasText) {
        // Speech and conversation buttons are only visible when there's no text
        btnStartSpeech.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnStartConversation.setVisibility(hasText ? View.GONE : View.VISIBLE);
        btnHistory.setVisibility(hasText ? View.GONE : View.VISIBLE);
        
        // Translate and clear buttons are only visible when there's text
        btnTranslate.setVisibility(hasText ? View.VISIBLE : View.GONE);
        btnClear.setVisibility(hasText ? View.VISIBLE : View.GONE);
        
        // Profile button is always visible
    }

    private void translateAnimation() {
        if (textViewResult == null) return;
        
        // Clear any existing text
        textViewResult.setText("");
        textViewResult.setVisibility(View.GONE);
        
        // Show the GIF animation
        translatingAnimation.setVisibility(View.VISIBLE);
        
        isTranslating = true;
    }

    private void stopAnimation() {
        translatingAnimation.setVisibility(View.GONE);
    }

    private void startTranslation() {
        isTranslating = true;
        
        float disabledAlpha = 0.5f;
        
        // Disable input field
        textInput.setEnabled(false);
        textInput.animate().alpha(disabledAlpha).setDuration(300);
        
        // Disable buttons
        btnTranslate.setEnabled(false);
        btnTranslate.animate().alpha(disabledAlpha).setDuration(300);
        
        btnClear.setEnabled(false);
        btnClear.animate().alpha(disabledAlpha).setDuration(300);
        
        btnStartConversation.setEnabled(false);
        btnStartConversation.animate().alpha(disabledAlpha).setDuration(300);
        
        btnStartSpeech.setEnabled(false);
        btnStartSpeech.animate().alpha(disabledAlpha).setDuration(300);
        
        btnHistory.setEnabled(false);
        btnHistory.animate().alpha(disabledAlpha).setDuration(300);
        
        // Profile button remains enabled during translation
        
        // Disable language selection
        outputLanguageSelection.setEnabled(false);
        outputLanguageSelection.animate().alpha(disabledAlpha).setDuration(300);
        
        // Show stop button
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
        // Enable input field
        textInput.setEnabled(true);
        textInput.animate().alpha(1f).setDuration(300);
        
        // Enable buttons
        btnTranslate.setEnabled(true);
        btnTranslate.animate().alpha(1f).setDuration(300);
        
        btnClear.setEnabled(true);
        btnClear.animate().alpha(1f).setDuration(300);
        
        btnStartConversation.setEnabled(true);
        btnStartConversation.animate().alpha(1f).setDuration(300);
        
        btnStartSpeech.setEnabled(true);
        btnStartSpeech.animate().alpha(1f).setDuration(300);
        
        btnHistory.setEnabled(true);
        btnHistory.animate().alpha(1f).setDuration(300);
        
        // Profile button is always enabled
        
        // Enable language selection
        outputLanguageSelection.setEnabled(true);
        outputLanguageSelection.animate().alpha(1f).setDuration(300);
        
        // Hide stop button
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

        // Create and execute the translator
        currentTranslator = TranslatorFactory.createTranslator(
                TranslatorType.fromId(getCurrentTranslator()),
                targetLanguage,
                translatedText -> {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            handleTranslationResult(translatedText);
                            isTranslating = false;

                            // Save to history
                            if (!translatedText.startsWith("Error:")) {
                                saveToHistory(text, translatedText, targetLanguage);
                            }
                        });
                    }
                },
                requireContext());
        
        // Execute the translation
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

    /**
     * Save the translation to history
     */
    private void saveToHistory(String originalText, String translatedText, String targetLanguage) {
        if (historyManager != null) {
            String sourceLanguage = Variables.userLanguage;
            String translator = getCurrentTranslator();
            TranslationHistory history = new TranslationHistory(
                originalText, translatedText, sourceLanguage, targetLanguage, translator);
            historyManager.saveTranslation(history);
        }
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
        
        // Remove Firebase listeners - only if not a guest user
        if (userRef != null && !"guest".equals(Variables.userUID) && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userRef.removeEventListener(userValueEventListener);
        }
        
        // Stop any ongoing animations
        if (animationHandler != null && animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }
        
        // Stop dots animation
        if (dotsHandler != null && dotsAnimation != null) {
            dotsHandler.removeCallbacks(dotsAnimation);
        }
        
        // Release speech recognizer
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        
        // Cancel any ongoing translation
        if (currentTranslator != null) {
            currentTranslator.cancel(true);
        }
    }

    // Helper methods to handle translation results
    private void handleTranslationResult(String translatedText) {
        // Stop animation
        stopAnimation();
        enableInputSection();
        
        if (translatedText.startsWith("Error:")) {
            handleTranslationError();
            return;
        }
        
        // Display result
        textViewResult.setVisibility(View.VISIBLE);
        textViewResult.setText(translatedText);
        textViewResult.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void handleTranslationError() {
        stopAnimation();
        textViewResult.setText("Translation failed");
        textViewResult.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        textViewResult.setTextSize(38);
        textViewResult.setAlpha(1.0f);
    }

    private void showTranslatorSelectionDialog(View anchorView) {
        Dialog translatorDialog = new Dialog(requireContext());
        translatorDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        translatorDialog.setContentView(R.layout.translator_selection_dialog);

        // Get dialog window and set properties
        Window window = translatorDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            
            // Get location of anchor view
            int[] location = new int[2];
            anchorView.getLocationInWindow(location);
            
            // Configure window layout
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = location[0];
            params.y = location[1] + anchorView.getHeight();
            window.setAttributes(params);
            
            // Set dialog width to match anchor view width
            window.setLayout(anchorView.getWidth(), LayoutParams.WRAP_CONTENT);
        }

        // Get container for translator buttons
        LinearLayout container = translatorDialog.findViewById(R.id.translatorButtonsContainer);
        
        // Setup translator buttons using the control
        translatorControl.setupTranslatorButtons(container, translatorDialog);

        translatorDialog.show();
    }

    private void showConversationalModeSelectionDialog() {
        Dialog modeDialog = new Dialog(requireContext());
        modeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        modeDialog.setContentView(R.layout.conversational_mode_selection_dialog);

        Window window = modeDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Setup mode selection buttons
        LinearLayout container = modeDialog.findViewById(R.id.modeSelectionContainer);

        // Single-Device Mode Button
        View singleDeviceButton = LayoutInflater.from(requireContext())
                .inflate(R.layout.conversational_mode_button, container, false);
        ((TextView) singleDeviceButton.findViewById(R.id.modeTitle)).setText("Single-Device Mode");
        ((TextView) singleDeviceButton.findViewById(R.id.modeDescription))
                .setText("Two users on one device with upside-down screen");
        ((ImageView) singleDeviceButton.findViewById(R.id.modeIcon))
                .setImageResource(R.drawable.ic_single_device);
        singleDeviceButton.setOnClickListener(v -> {
            modeDialog.dismiss();
            startSingleDeviceConversationalMode();
        });
        container.addView(singleDeviceButton);

        // Connect with Other Users Mode Button
        View connectUsersButton = LayoutInflater.from(requireContext())
                .inflate(R.layout.conversational_mode_button, container, false);
        ((TextView) connectUsersButton.findViewById(R.id.modeTitle)).setText("Connect with Other Users");
        ((TextView) connectUsersButton.findViewById(R.id.modeDescription))
                .setText("Voice chat with remote users via Firebase");
        ((ImageView) connectUsersButton.findViewById(R.id.modeIcon))
                .setImageResource(R.drawable.ic_connect_users);
        connectUsersButton.setOnClickListener(v -> {
            modeDialog.dismiss();
            startConnectUsersConversationalMode();
        });
        container.addView(connectUsersButton);

        modeDialog.show();
    }

    private void startSingleDeviceConversationalMode() {
        View rootView = getView();
        if (rootView != null) {
            // Disable interaction immediately
            rootView.setClickable(false);
            rootView.setEnabled(false);

            // Use ViewPropertyAnimator for smoother fade out
            rootView.animate()
                   .alpha(0f)
                   .setDuration(300)
                   .withEndAction(() -> {
                       Intent intent = new Intent(requireContext(), ConversationalActivity.class);
                       startActivity(intent);
                       requireActivity().overridePendingTransition(0, 0);
                   })
                   .start();
        }
    }

    private void startConnectUsersConversationalMode() {
        showUserConnectionOptionsDialog();
    }

    private void showUserConnectionOptionsDialog() {
        Dialog connectionDialog = new Dialog(requireContext());
        connectionDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        connectionDialog.setContentView(R.layout.user_connection_options_dialog);

        Window window = connectionDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Initialize QR code view
        ImageView userQRCodeImage = connectionDialog.findViewById(R.id.userQRCodeImage);

        // Generate and display user's QR code
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && !"guest".equals(Variables.userUID)) {
            com.example.appdev.utils.QRCodeGenerator qrGenerator = new com.example.appdev.utils.QRCodeGenerator();
            android.graphics.Bitmap qrBitmap = qrGenerator.generateUserQRCode(
                    Variables.userUID,
                    Variables.userDisplayName,
                    Variables.userLanguage,
                    null); // Profile image URL can be added later

            if (qrBitmap != null) {
                userQRCodeImage.setImageBitmap(qrBitmap);
            } else {
                CustomNotification.showNotification(requireContext(),
                        "Failed to generate QR code", false);
            }
        } else {
            // Show login required message
            CustomNotification.showNotification(requireContext(),
                    "Login required to display QR code", false);
        }

        // Set up click listeners
        View scanQRButton = connectionDialog.findViewById(R.id.scanQRButton);
        View searchUsersButton = connectionDialog.findViewById(R.id.searchUsersButton);
        ImageButton closeButton = connectionDialog.findViewById(R.id.closeButton);

        scanQRButton.setOnClickListener(v -> {
            connectionDialog.dismiss();
            startQRScanForConnection();
        });

        searchUsersButton.setOnClickListener(v -> {
            connectionDialog.dismiss();
            startUserSearchForConnection();
        });

        closeButton.setOnClickListener(v -> connectionDialog.dismiss());

        connectionDialog.show();
    }


    private void startQRScanForConnection() {
        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || "guest".equals(Variables.userUID)) {
            CustomNotification.showNotification(requireContext(),
                    "You need to be logged in to scan QR codes", false);
            return;
        }

        // Start QR scan activity
        Intent intent = new Intent(requireContext(), QRScanActivity.class);
        startActivity(intent);
    }

    private void startUserSearchForConnection() {
        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || "guest".equals(Variables.userUID)) {
            CustomNotification.showNotification(requireContext(),
                    "You need to be logged in to search for users", false);
            return;
        }

        // Navigate to SearchUsersActivity
        Intent intent = new Intent(requireContext(), com.example.appdev.SearchUsersActivity.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        View rootView = getView();
        if (rootView != null) {
            // Reset alpha and enable interaction
            rootView.setAlpha(1f);
            rootView.setEnabled(true);
            rootView.setClickable(true);
        }
    }

    private void showLanguageSelectionDialog() {
        Dialog languageDialog = new Dialog(requireContext());
        languageDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        languageDialog.setContentView(R.layout.language_selection_dialog);

        // Get dialog window and set properties
        Window window = languageDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 
                            ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Get container for language buttons
        LinearLayout container = languageDialog.findViewById(R.id.languageButtonsContainer);
        
        // Setup language buttons
        List<String> languages = Languages.getAllLanguages();
        for (String language : languages) {
            View languageButton = LayoutInflater.from(requireContext()).inflate(R.layout.language_button, container, false);
            ((TextView) languageButton.findViewById(R.id.languageText)).setText(language);
            languageButton.setOnClickListener(v -> {
                Variables.userLanguage = language;
                currentLanguageLabel.setText(language);
                setupLanguageSpinners();
                languageDialog.dismiss();
            });
            container.addView(languageButton);
        }

        languageDialog.show();
    }

    /**
     * Toggle between formal and casual translation modes
     */
    private void toggleFormality() {
        // Toggle the formality mode
        Variables.isFormalTranslationMode = !Variables.isFormalTranslationMode;
        
        // Update shared preferences
        TranslationModeManager.saveToPreferences(requireContext(), Variables.isFormalTranslationMode);

        // Show feedback to the user
        String feedbackMessage = Variables.isFormalTranslationMode ? 
                                "Formal Translation" : "Casual Translation";
        
        // Show text feedback instead of toast
        showModeFeedback(feedbackMessage);
    }
    
    /**
     * Update the formality toggle button icon based on current mode
     */
    private void openProfile() {
        // Open ProfileFragment
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openProfileFragment();
        }
    }

    private void loadUserProfilePicture() {
        if (profileButton != null && getActivity() != null) {
            // Load user's profile picture
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("profileImageUrl");

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String profileImageUrl = dataSnapshot.getValue(String.class);
                        if (profileImageUrl != null && !profileImageUrl.equals("none") && !profileImageUrl.isEmpty()) {
                            Glide.with(getActivity())
                                .load(profileImageUrl)
                                .placeholder(R.drawable.default_userpic)
                                .error(R.drawable.default_userpic)
                                .into(profileButton);
                        } else {
                            profileButton.setImageResource(R.drawable.default_userpic);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        profileButton.setImageResource(R.drawable.default_userpic);
                    }
                });
            } else {
                profileButton.setImageResource(R.drawable.default_userpic);
            }
        }
    }

    private void showModeFeedback(String message) {
        // Cancel any pending feedback dismissal
        feedbackHandler.removeCallbacksAndMessages(null);
        
        // Set the feedback message
        modeFeedbackText.setText(message);
        
        // Set background color based on mode
        int backgroundColor = Variables.isFormalTranslationMode ? 
                            Color.parseColor("#3F51B5") : Color.parseColor("#FF9800");
        GradientDrawable background = (GradientDrawable) modeFeedbackText.getBackground();
        background.setColor(backgroundColor);
        
        // Make the feedback visible
        modeFeedbackText.setVisibility(View.VISIBLE);
        modeFeedbackText.setAlpha(0f);
        
        // Animate the feedback in
        modeFeedbackText.animate()
                .alpha(1f)
                .setDuration(250)
                .start();
        
        // Schedule feedback to disappear after 2 seconds
        feedbackHandler.postDelayed(() -> {
            modeFeedbackText.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> modeFeedbackText.setVisibility(View.GONE))
                    .start();
        }, 2000);
    }
}