package com.example.appdev.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.appdev.R;
import com.google.android.material.button.MaterialButton;

public class SpeechRecognitionDialog extends Dialog {
    private TextView recognizedText;
    private MaterialButton btnCancel, btnDone;
    private ImageView pulseCircle1, pulseCircle2;
    private SpeechRecognitionListener listener;
    private ObjectAnimator scaleX1, scaleY1, alpha1;
    private ObjectAnimator scaleX2, scaleY2, alpha2;
    private boolean isListening = false;
    private boolean animationInitialized = false;
    private boolean isUpsideDown = false;

    public interface SpeechRecognitionListener {
        void onCancelled();
        void onFinished(String text);
    }

    public SpeechRecognitionDialog(@NonNull Context context, SpeechRecognitionListener listener, boolean isUpsideDown) {
        super(context);
        this.listener = listener;
        this.isUpsideDown = isUpsideDown;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.speech_recognition_dialog);

        recognizedText = findViewById(R.id.recognizedText);
        btnCancel = findViewById(R.id.btnCancel);
        btnDone = findViewById(R.id.btnDone);
        pulseCircle1 = findViewById(R.id.pulseCircle1);
        pulseCircle2 = findViewById(R.id.pulseCircle2);

        // If it's for User 2 (top), rotate the entire dialog window
        if (isUpsideDown) {
            Window window = getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                decorView.setRotation(180);
                
                // Adjust window position to maintain center alignment after rotation
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.CENTER;
                window.setAttributes(params);
            }
        }

        setupPulseAnimation();

        btnCancel.setOnClickListener(v -> {
            listener.onCancelled();
            dismiss();
        });

        btnDone.setOnClickListener(v -> {
            listener.onFinished(recognizedText.getText().toString());
            dismiss();
        });

        setCancelable(false);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    private void setupPulseAnimation() {
        try {
            // First circle animation
            scaleX1 = ObjectAnimator.ofFloat(pulseCircle1, "scaleX", 1f, 1.5f);
            scaleY1 = ObjectAnimator.ofFloat(pulseCircle1, "scaleY", 1f, 1.5f);
            alpha1 = ObjectAnimator.ofFloat(pulseCircle1, "alpha", 0.5f, 0f);

            // Second circle animation
            scaleX2 = ObjectAnimator.ofFloat(pulseCircle2, "scaleX", 1f, 1.5f);
            scaleY2 = ObjectAnimator.ofFloat(pulseCircle2, "scaleY", 1f, 1.5f);
            alpha2 = ObjectAnimator.ofFloat(pulseCircle2, "alpha", 0.5f, 0f);

            AnimatorSet set1 = new AnimatorSet();
            set1.playTogether(scaleX1, scaleY1, alpha1);
            set1.setDuration(1500);
            set1.setInterpolator(new LinearInterpolator());
            set1.setStartDelay(0);

            AnimatorSet set2 = new AnimatorSet();
            set2.playTogether(scaleX2, scaleY2, alpha2);
            set2.setDuration(1500);
            set2.setInterpolator(new LinearInterpolator());
            set2.setStartDelay(750);

            set1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!isShowing()) return;
                    set1.start();
                }
            });

            set2.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!isShowing()) return;
                    set2.start();
                }
            });

            set1.start();
            set2.start();
            animationInitialized = true;
        } catch (Exception e) {
            // Handle any animation setup errors
            animationInitialized = false;
        }
    }

    public void updateRecognizedText(String text) {
        if (recognizedText != null) {
            recognizedText.setText(text);
        }
    }

    public void updateVoiceAnimation(float rmsdB) {
        if (!animationInitialized || !isShowing()) return;

        try {
            if (rmsdB > 1.0f && !isListening) {
                // Voice detected, switch to blue
                isListening = true;
                pulseCircle1.setBackgroundResource(R.drawable.mic_circle_background_blue);
                pulseCircle2.setBackgroundResource(R.drawable.mic_circle_background_blue);
            } else if (rmsdB <= 1.0f && isListening) {
                // No voice, switch back to orange
                isListening = false;
                pulseCircle1.setBackgroundResource(R.drawable.mic_circle_background);
                pulseCircle2.setBackgroundResource(R.drawable.mic_circle_background);
            }

            float scale = 1f + (rmsdB * 0.1f);
            scale = Math.min(Math.max(scale, 1f), 2f);
            
            if (scaleX1 != null && scaleY1 != null && scaleX2 != null && scaleY2 != null) {
                scaleX1.setFloatValues(1f, scale * 1.5f);
                scaleY1.setFloatValues(1f, scale * 1.5f);
                scaleX2.setFloatValues(1f, scale * 1.5f);
                scaleY2.setFloatValues(1f, scale * 1.5f);
            }
        } catch (Exception e) {
            // Handle any animation update errors silently
        }
    }

    @Override
    public void dismiss() {
        isListening = false;
        animationInitialized = false;
        if (scaleX1 != null) {
            try {
                scaleX1.cancel();
                scaleY1.cancel();
                alpha1.cancel();
                scaleX2.cancel();
                scaleY2.cancel();
                alpha2.cancel();
            } catch (Exception e) {
                // Handle animation cancellation errors silently
            }
        }
        super.dismiss();
    }
} 