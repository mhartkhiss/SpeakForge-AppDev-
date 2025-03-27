package com.example.appdev;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import android.widget.AdapterView;
import android.os.AsyncTask;
import android.os.Build;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.speech.RecognizerIntent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.widget.ImageView;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Handler;
import android.view.animation.Interpolator;

import com.example.appdev.models.Languages;
import com.example.appdev.utils.SpeechRecognitionDialog;
import com.example.appdev.utils.SpeechRecognitionHelper;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.appdev.translators.TranslatorFactory;
import com.example.appdev.translators.TranslatorType;
import com.example.appdev.utils.ConversationalSpeechRecognizer;
import com.example.appdev.utils.LoadingDotsView;

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
    private ConversationalSpeechRecognizer speechRecognizer;
    private static final int PERMISSION_REQUEST_CODE = 123;

    private boolean isUser1Speaking = false;
    private boolean isUser2Speaking = false;
    private Drawable micIcon;
    private Drawable stopIcon;

    // Loading dots
    private LoadingDotsView user1LoadingDots;
    private LoadingDotsView user2LoadingDots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make the window background light
        getWindow().setBackgroundDrawableResource(android.R.color.white);
        
        // Add full screen flags
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Hide system bars
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        
        setContentView(R.layout.activity_conversational);

        // Get references to the top and bottom sections
        View topSection = findViewById(R.id.user2Section);
        View bottomSection = findViewById(R.id.user1Section);
        View centerDivider = findViewById(R.id.centerDivider);

        // Initially hide the sections
        topSection.setVisibility(View.INVISIBLE);
        bottomSection.setVisibility(View.INVISIBLE);
        centerDivider.setAlpha(0f);

        // Load animations
        Animation slideInTop = AnimationUtils.loadAnimation(this, R.anim.slide_fade_in_top);
        Animation slideInBottom = AnimationUtils.loadAnimation(this, R.anim.slide_fade_in_bottom);

        // Post the animations to ensure views are properly laid out
        topSection.post(() -> {
            // Show and animate top section
            topSection.setVisibility(View.VISIBLE);
            topSection.startAnimation(slideInTop);
        });

        bottomSection.post(() -> {
            // Show and animate bottom section
            bottomSection.setVisibility(View.VISIBLE);
            bottomSection.startAnimation(slideInBottom);
        });

        // Fade in the center divider
        centerDivider.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(400)
            .start();

        // Initialize User 1 (Bottom) Views
        user1Result = findViewById(R.id.user1Result);
        user1LanguageSpinner = findViewById(R.id.user1LanguageSpinner);
        user1SpeakButton = findViewById(R.id.user1SpeakButton);

        // Initialize User 2 (Top) Views
        user2Result = findViewById(R.id.user2Result);
        user2LanguageSpinner = findViewById(R.id.user2LanguageSpinner);
        user2SpeakButton = findViewById(R.id.user2SpeakButton);

        // Initialize text views with empty state
        updateTextView(user1Result, "", false);
        updateTextView(user2Result, "", false);

        // Initialize Speech Recognition
        speechRecognizer = new ConversationalSpeechRecognizer(this);

        // Initialize icons
        micIcon = ContextCompat.getDrawable(this, R.drawable.ic_mic);
        stopIcon = ContextCompat.getDrawable(this, R.drawable.ic_stop);

        // Initialize loading dots
        user1LoadingDots = findViewById(R.id.user1LoadingDots);
        user2LoadingDots = findViewById(R.id.user2LoadingDots);

        // Initialize conversational icon
        ImageView conversationalIcon = findViewById(R.id.conversationalIcon);
        conversationalIcon.setOnClickListener(v -> {
            finish(); // This will close the activity
        });

        // Setup language spinners
        setupLanguageSpinners();

        // Setup click listeners
        setupClickListeners();
    }

    private void setupLanguageSpinners() {
        // Setup User 1 Spinner (Orange user)
        ArrayAdapter<String> user1Adapter = new ArrayAdapter<String>(this,
            R.layout.simple_spinner_item_custom, Languages.getUser1Languages()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(ContextCompat.getColor(getContext(), R.color.user1_color));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(ContextCompat.getColor(getContext(), R.color.user1_color));
                return view;
            }
        };
        user1Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        user1LanguageSpinner.setAdapter(user1Adapter);
        
        // Set default selection for User 1
        int user1Position = user1Adapter.getPosition(Languages.getUser1Language());
        if (user1Position >= 0) {
            user1LanguageSpinner.setSelection(user1Position);
        }

        // Setup User 2 Spinner (Blue user)
        ArrayAdapter<String> user2Adapter = new ArrayAdapter<String>(this,
            R.layout.simple_spinner_item_custom, Languages.getUser2Languages()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(ContextCompat.getColor(getContext(), R.color.user2_color));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(ContextCompat.getColor(getContext(), R.color.user2_color));
                return view;
            }
        };
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
            if (!isUser1Speaking) {
                startSpeechRecognition(true);
            } else {
                stopSpeechRecognition(true);
            }
        });

        user2SpeakButton.setOnClickListener(v -> {
            if (!isUser2Speaking) {
                startSpeechRecognition(false);
            } else {
                stopSpeechRecognition(false);
            }
        });
    }

    private void startSpeechRecognition(boolean isUser1) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionResult = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO);
            if (permissionResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startListening(isUser1);
            } else {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            }
        } else {
            startListening(isUser1);
        }
    }

    private void startListening(boolean isUser1) {
        // Update UI state
        if (isUser1) {
            isUser1Speaking = true;
            user1SpeakButton.setImageDrawable(stopIcon);
            pulseAnimation(user1SpeakButton);
            updateTextView(user1Result, "", false);
            
            // Show loading dots for User 2 with User 1's color
            user2LoadingDots.setDotColor(ContextCompat.getColor(this, R.color.user1_color));
            user2LoadingDots.setVisibility(View.VISIBLE);
            user2LoadingDots.startAnimation();
            user2Result.setVisibility(View.GONE);
            
            // Disable User 2's controls
            user2SpeakButton.setEnabled(false);
            user2SpeakButton.setAlpha(0.5f);
            user2LanguageSpinner.setEnabled(false);
            user2LanguageSpinner.setAlpha(0.5f);
        } else {
            isUser2Speaking = true;
            user2SpeakButton.setImageDrawable(stopIcon);
            pulseAnimation(user2SpeakButton);
            updateTextView(user2Result, "", false);
            
            // Show loading dots for User 1 with User 2's color
            user1LoadingDots.setDotColor(ContextCompat.getColor(this, R.color.user2_color));
            user1LoadingDots.setVisibility(View.VISIBLE);
            user1LoadingDots.startAnimation();
            user1Result.setVisibility(View.GONE);
            
            // Disable User 1's controls
            user1SpeakButton.setEnabled(false);
            user1SpeakButton.setAlpha(0.5f);
            user1LanguageSpinner.setEnabled(false);
            user1LanguageSpinner.setAlpha(0.5f);
        }

        // Get selected language for speech recognition
        String language = isUser1 ? 
            user1LanguageSpinner.getSelectedItem().toString() : 
            user2LanguageSpinner.getSelectedItem().toString();

        speechRecognizer.startListening(language, new ConversationalSpeechRecognizer.OnSpeechResultListener() {
            @Override
            public void onPartialResult(String text) {
                runOnUiThread(() -> {
                    if (isUser1) {
                        updateTextView(user1Result, text, false); // Input text, no background
                    } else {
                        updateTextView(user2Result, text, false); // Input text, no background
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    CustomNotification.showNotification(ConversationalActivity.this, errorMessage, false);
                    stopSpeechRecognition(isUser1);
                });
            }
        });
    }

    private void stopSpeechRecognition(boolean isUser1) {
        speechRecognizer.stopListening();

        if (isUser1) {
            isUser1Speaking = false;
            user1SpeakButton.setImageDrawable(micIcon);
            user1SpeakButton.clearAnimation();
            
            // Hide User 2's loading dots
            user2LoadingDots.stopAnimation();
            user2LoadingDots.setVisibility(View.GONE);
            user2Result.setVisibility(View.VISIBLE);
            
            // Re-enable User 2's controls
            user2SpeakButton.setEnabled(true);
            user2SpeakButton.setAlpha(1.0f);
            user2LanguageSpinner.setEnabled(true);
            user2LanguageSpinner.setAlpha(1.0f);
            
            String text = user1Result.getText().toString();
            if (!text.isEmpty()) {
                String targetLanguage = user2LanguageSpinner.getSelectedItem().toString();
                translateAndDisplay(text, targetLanguage, true);
            }
        } else {
            isUser2Speaking = false;
            user2SpeakButton.setImageDrawable(micIcon);
            user2SpeakButton.clearAnimation();
            
            // Hide User 1's loading dots
            user1LoadingDots.stopAnimation();
            user1LoadingDots.setVisibility(View.GONE);
            user1Result.setVisibility(View.VISIBLE);
            
            // Re-enable User 1's controls
            user1SpeakButton.setEnabled(true);
            user1SpeakButton.setAlpha(1.0f);
            user1LanguageSpinner.setEnabled(true);
            user1LanguageSpinner.setAlpha(1.0f);
            
            String text = user2Result.getText().toString();
            if (!text.isEmpty()) {
                String targetLanguage = user1LanguageSpinner.getSelectedItem().toString();
                translateAndDisplay(text, targetLanguage, false);
            }
        }
    }

    private void pulseAnimation(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f);
        
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);
        
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void updateTextView(TextView textView, String text, boolean isOutput) {
        boolean isUser1 = (textView == user1Result);
        
        if (isOutput && !text.isEmpty()) {
            // Set the appropriate speech bubble background
            textView.setBackground(ContextCompat.getDrawable(this,
                isUser1 ? R.drawable.speech_bubble_user1 : R.drawable.speech_bubble_user2));
            // Add padding for better appearance
            textView.setPadding(
                dpToPx(24), // left
                dpToPx(24), // top
                dpToPx(24), // right
                dpToPx(24)  // bottom
            );
            // Use white text for better contrast on colored backgrounds
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        } else {
            // Reset background and padding for input text or empty text
            textView.setBackground(null);
            textView.setPadding(
                dpToPx(16), // left
                dpToPx(16), // top
                dpToPx(16), // right
                dpToPx(16)  // bottom
            );
            // Use user's color for input text
            textView.setTextColor(ContextCompat.getColor(this, 
                isUser1 ? R.color.user1_color : R.color.user2_color));
        }
        
        textView.setText(text);
    }

    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void translateAndDisplay(String text, String targetLanguage, boolean isUser1) {
        // Start translation UI state
        startTranslation(isUser1);

        // Show loading dots for translation
        if (isUser1) {
            user2LoadingDots.setDotColor(ContextCompat.getColor(this, R.color.user1_color));
            user2LoadingDots.setVisibility(View.VISIBLE);
            user2LoadingDots.startAnimation();
            user2Result.setVisibility(View.GONE);
        } else {
            user1LoadingDots.setDotColor(ContextCompat.getColor(this, R.color.user2_color));
            user1LoadingDots.setVisibility(View.VISIBLE);
            user1LoadingDots.startAnimation();
            user1Result.setVisibility(View.GONE);
        }

        // Create and execute translator
        AsyncTask<String, Void, String> translator = TranslatorFactory.createTranslator(
            TranslatorType.fromId(Variables.userTranslator),
            targetLanguage,
            translatedMessage -> {
                if (!isFinishing()) {
                    runOnUiThread(() -> {
                        if (isUser1) {
                            // Hide loading dots and show result
                            user2LoadingDots.stopAnimation();
                            user2LoadingDots.setVisibility(View.GONE);
                            user2Result.setVisibility(View.VISIBLE);
                            updateTextView(user2Result, translatedMessage, true);
                        } else {
                            // Hide loading dots and show result
                            user1LoadingDots.stopAnimation();
                            user1LoadingDots.setVisibility(View.GONE);
                            user1Result.setVisibility(View.VISIBLE);
                            updateTextView(user1Result, translatedMessage, true);
                        }
                        enableControls(isUser1);
                    });
                }
            },
            this
        );
        translator.execute(text);
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
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        user1SpeakButton.clearAnimation();
        user2SpeakButton.clearAnimation();
        user1LoadingDots.stopAnimation();
        user2LoadingDots.stopAnimation();
    }

    @Override
    public void finish() {
        super.finish();
        // Disable default animation when going back
        overridePendingTransition(0, 0);
    }

    @Override
    public void onBackPressed() {
        // Get references to views
        View topSection = findViewById(R.id.user2Section);
        View bottomSection = findViewById(R.id.user1Section);
        View centerDivider = findViewById(R.id.centerDivider);

        // Create and start exit animations
        Animation slideOutTop = AnimationUtils.loadAnimation(this, R.anim.slide_fade_in_top);
        slideOutTop.setDuration(400);
        slideOutTop.setInterpolator(new ReverseInterpolator());
        
        Animation slideOutBottom = AnimationUtils.loadAnimation(this, R.anim.slide_fade_in_bottom);
        slideOutBottom.setDuration(400);
        slideOutBottom.setInterpolator(new ReverseInterpolator());

        // Start animations
        topSection.startAnimation(slideOutTop);
        bottomSection.startAnimation(slideOutBottom);
        
        // Fade out center divider
        centerDivider.animate()
            .alpha(0f)
            .setDuration(300)
            .start();

        // Finish activity after animation
        new Handler().postDelayed(this::finish, 350);
    }

    // Custom interpolator to reverse animations
    private static class ReverseInterpolator implements Interpolator {
        @Override
        public float getInterpolation(float input) {
            return Math.abs(input - 1f);
        }
    }
} 