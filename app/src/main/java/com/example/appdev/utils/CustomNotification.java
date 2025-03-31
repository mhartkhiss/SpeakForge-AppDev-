package com.example.appdev.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appdev.R;

public class CustomNotification {
    
    private static Toast currentToast;
    
    public static void showNotification(Context context, String message, boolean isSuccess) {
        String title = isSuccess ? "Success" : "Failed";
        showNotification(context, title, message, isSuccess);
    }
    
    public static void showNotification(Context context, String title, String message, boolean isSuccess) {
        // Cancel any existing toast to prevent stacking
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        // Inflate the custom layout
        View layout = LayoutInflater.from(context).inflate(R.layout.custom_notification, null);

        // Get views
        ImageView icon = layout.findViewById(R.id.notificationIcon);
        TextView titleView = layout.findViewById(R.id.notificationTitle);
        TextView textView = layout.findViewById(R.id.notificationText);
        ImageView closeButton = layout.findViewById(R.id.closeNotification);

        // Set icon, title and text
        icon.setImageResource(isSuccess ? R.drawable.ic_success : R.drawable.ic_error);
        titleView.setText(title);
        textView.setText(message);
        
        // Apply tint to match the notification style
        icon.setColorFilter(context.getResources().getColor(isSuccess ? R.color.success_green : R.color.error_red));

        // Create toast
        currentToast = new Toast(context);
        currentToast.setGravity(Gravity.TOP, 0, 64);
        currentToast.setDuration(Toast.LENGTH_LONG);
        currentToast.setView(layout);
        
        // Set up close button
        closeButton.setOnClickListener(v -> {
            if (currentToast != null) {
                animateAndDismiss(layout);
            }
        });
        
        // Apply entry animation
        layout.setAlpha(0f);
        layout.setTranslationY(-50f);
        
        // Show toast
        currentToast.show();
        
        // Apply animation after toast is shown
        layout.animate()
              .alpha(1f)
              .translationY(0f)
              .setDuration(300)
              .setInterpolator(new DecelerateInterpolator())
              .start();
        
        // Auto dismiss after a delay
        new Handler().postDelayed(() -> {
            if (currentToast != null) {
                animateAndDismiss(layout);
            }
        }, 4000); // Dismiss after 4 seconds
    }
    
    // Add a new method for showing notifications with specific status types
    public static void showStatusNotification(Context context, String message, NotificationType type) {
        String title;
        int iconRes;
        int colorRes;
        
        switch (type) {
            case SUCCESS:
                title = "Success";
                iconRes = R.drawable.ic_success;
                colorRes = R.color.success_green;
                break;
            case ERROR:
                title = "Failed";
                iconRes = R.drawable.ic_error;
                colorRes = R.color.error_red;
                break;
            case WARNING:
                title = "Warning";
                iconRes = R.drawable.ic_warning;
                colorRes = R.color.casual_mode_bg; // Orange color
                break;
            case INFO:
                title = "Information";
                iconRes = R.drawable.ic_info;
                colorRes = R.color.blue;
                break;
            default:
                title = "Notification";
                iconRes = R.drawable.ic_info;
                colorRes = R.color.blue;
                break;
        }
        
        showCustomNotification(context, title, message, iconRes, colorRes);
    }
    
    private static void showCustomNotification(Context context, String title, String message, int iconRes, int colorRes) {
        // Cancel any existing toast to prevent stacking
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        // Inflate the custom layout
        View layout = LayoutInflater.from(context).inflate(R.layout.custom_notification, null);

        // Get views
        ImageView icon = layout.findViewById(R.id.notificationIcon);
        TextView titleView = layout.findViewById(R.id.notificationTitle);
        TextView textView = layout.findViewById(R.id.notificationText);
        ImageView closeButton = layout.findViewById(R.id.closeNotification);

        // Set icon, title and text
        icon.setImageResource(iconRes);
        titleView.setText(title);
        textView.setText(message);
        
        // Apply tint to match the notification style
        icon.setColorFilter(context.getResources().getColor(colorRes));

        // Create toast
        currentToast = new Toast(context);
        currentToast.setGravity(Gravity.TOP, 0, 64);
        currentToast.setDuration(Toast.LENGTH_LONG);
        currentToast.setView(layout);
        
        // Set up close button
        closeButton.setOnClickListener(v -> {
            if (currentToast != null) {
                animateAndDismiss(layout);
            }
        });
        
        // Apply entry animation
        layout.setAlpha(0f);
        layout.setTranslationY(-50f);
        
        // Show toast
        currentToast.show();
        
        // Apply animation after toast is shown
        layout.animate()
              .alpha(1f)
              .translationY(0f)
              .setDuration(300)
              .setInterpolator(new DecelerateInterpolator())
              .start();
        
        // Auto dismiss after a delay
        new Handler().postDelayed(() -> {
            if (currentToast != null) {
                animateAndDismiss(layout);
            }
        }, 4000); // Dismiss after 4 seconds
    }
    
    private static void animateAndDismiss(View layout) {
        layout.animate()
              .alpha(0f)
              .translationY(-50f)
              .setDuration(200)
              .setListener(new AnimatorListenerAdapter() {
                  @Override
                  public void onAnimationEnd(Animator animation) {
                      if (currentToast != null) {
                          currentToast.cancel();
                          currentToast = null;
                      }
                  }
              })
              .start();
    }
    
    // Enum for notification types
    public enum NotificationType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO
    }
}