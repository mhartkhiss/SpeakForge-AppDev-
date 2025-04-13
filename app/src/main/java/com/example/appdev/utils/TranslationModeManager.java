package com.example.appdev.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.cardview.widget.CardView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

import com.example.appdev.R;
import com.example.appdev.Variables;

/**
 * Utility class to manage translation mode (formal/casual) settings
 * and provide a floating toggle button for this feature.
 */
public class TranslationModeManager {
    
    /**
     * Interface to handle translation mode changes
     */
    public interface TranslationModeChangeListener {
        void onTranslationModeChanged(boolean isFormalMode);
    }
    
    /**
     * Initializes translation mode from SharedPreferences
     */
    public static void initializeFromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Variables.PREFS_NAME, Context.MODE_PRIVATE);
        Variables.isFormalTranslationMode = prefs.getBoolean(Variables.PREF_FORMAL_TRANSLATION_MODE, false);
    }
    
    /**
     * Save translation mode to SharedPreferences
     */
    public static void saveToPreferences(Context context, boolean isFormalMode) {
        SharedPreferences prefs = context.getSharedPreferences(Variables.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Variables.PREF_FORMAL_TRANSLATION_MODE, isFormalMode);
        editor.apply();
        Variables.isFormalTranslationMode = isFormalMode;
    }
    
    /**
     * Shows a dialog to select translation mode (formal or casual)
     * @param context The context
     * @param listener Listener to handle translation mode changes
     */
    public static void showTranslationModeDialog(Context context, TranslationModeChangeListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Translation Mode");
        
        // Set up the options
        String[] modes = {"Casual Translation", "Formal Translation"};
        int checkedItem = Variables.isFormalTranslationMode ? 1 : 0;
        
        builder.setSingleChoiceItems(modes, checkedItem, (dialog, which) -> {
            boolean isFormalMode = (which == 1);
            
            // Notify listener
            if (listener != null) {
                listener.onTranslationModeChanged(isFormalMode);
            }
            
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Creates and attaches a floating translation mode toggle to a parent view
     * @param context The context
     * @param parent The parent view to attach the toggle to
     * @param listener Listener to handle translation mode changes
     * @return The created toggle view
     */
    public static View createToggleButton(Context context, ViewGroup parent, TranslationModeChangeListener listener) {
        View toggleView = LayoutInflater.from(context).inflate(R.layout.translation_mode_toggle, parent, false);
        
        CardView toggleCard = toggleView.findViewById(R.id.translationModeToggle);
        ImageView modeIcon = toggleView.findViewById(R.id.modeIcon);
        TextView modeLabel = toggleView.findViewById(R.id.modeLabel);
        
        // Set initial state
        updateToggleAppearance(context, toggleCard, modeIcon, modeLabel, Variables.isFormalTranslationMode);
        
        // Set click listener
        toggleCard.setOnClickListener(v -> {
            boolean newMode = !Variables.isFormalTranslationMode;
            Variables.isFormalTranslationMode = newMode;
            saveToPreferences(context, newMode);
            updateToggleAppearance(context, toggleCard, modeIcon, modeLabel, newMode);
            
            if (listener != null) {
                listener.onTranslationModeChanged(newMode);
            }
        });
        
        parent.addView(toggleView);
        return toggleView;
    }
    
    /**
     * Updates the toggle button appearance based on the current mode
     */
    private static void updateToggleAppearance(Context context, CardView card, ImageView icon, TextView label, boolean isFormalMode) {
        if (isFormalMode) {
            card.setCardBackgroundColor(context.getResources().getColor(R.color.formal_mode_background));
            icon.setImageResource(R.drawable.translation_mode_formal);
            label.setText("Formal");
        } else {
            card.setCardBackgroundColor(context.getResources().getColor(R.color.casual_mode_background));
            icon.setImageResource(R.drawable.translation_mode_casual);
            label.setText("Casual");
        }
    }
} 