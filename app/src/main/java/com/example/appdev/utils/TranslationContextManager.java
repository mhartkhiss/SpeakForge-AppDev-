package com.example.appdev.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.example.appdev.R;
import com.example.appdev.Variables;

/**
 * Utility class to manage context-aware translation settings
 * including enabling/disabling and context depth control
 */
public class TranslationContextManager {

    /**
     * Interface to handle context setting changes
     */
    public interface ContextSettingsChangeListener {
        void onContextSettingsChanged(boolean isEnabled, int contextDepth);
    }
    
    /**
     * Initializes context awareness settings from SharedPreferences
     */
    public static void initializeFromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Variables.PREFS_NAME, Context.MODE_PRIVATE);
        Variables.isContextAwareTranslation = prefs.getBoolean(Variables.PREF_CONTEXT_AWARE_TRANSLATION, true);
        Variables.contextDepth = prefs.getInt(Variables.PREF_CONTEXT_DEPTH, 5);
    }
    
    /**
     * Save context awareness settings to SharedPreferences
     */
    public static void saveToPreferences(Context context, boolean isEnabled, int contextDepth) {
        SharedPreferences prefs = context.getSharedPreferences(Variables.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Variables.PREF_CONTEXT_AWARE_TRANSLATION, isEnabled);
        editor.putInt(Variables.PREF_CONTEXT_DEPTH, contextDepth);
        editor.apply();
        
        Variables.isContextAwareTranslation = isEnabled;
        Variables.contextDepth = contextDepth;
    }
    
    /**
     * Shows a dialog to configure context-aware translation settings
     */
    public static void showContextSettingsDialog(Context context, ContextSettingsChangeListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Context-Aware Translation");
        
        // Inflate custom layout
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_context_settings, null);
        builder.setView(dialogView);
        
        // Get views
        Switch switchContextEnabled = dialogView.findViewById(R.id.switch_context_enabled);
        LinearLayout layoutDepthControl = dialogView.findViewById(R.id.layout_depth_control);
        SeekBar seekBarDepth = dialogView.findViewById(R.id.seekbar_context_depth);
        TextView textViewDepthValue = dialogView.findViewById(R.id.text_depth_value);
        
        // Set initial values
        switchContextEnabled.setChecked(Variables.isContextAwareTranslation);
        layoutDepthControl.setVisibility(Variables.isContextAwareTranslation ? View.VISIBLE : View.GONE);
        seekBarDepth.setProgress(Variables.contextDepth);
        textViewDepthValue.setText(String.valueOf(Variables.contextDepth));
        
        // Listen for switch changes
        switchContextEnabled.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            layoutDepthControl.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        
        // Listen for seekbar changes
        seekBarDepth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = Math.max(1, progress); // Minimum of 1 message
                textViewDepthValue.setText(String.valueOf(value));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Set buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            boolean isEnabled = switchContextEnabled.isChecked();
            int depth = Math.max(1, seekBarDepth.getProgress());
            
            // Save to preferences
            saveToPreferences(context, isEnabled, depth);
            
            // Notify listener
            if (listener != null) {
                listener.onContextSettingsChanged(isEnabled, depth);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
} 