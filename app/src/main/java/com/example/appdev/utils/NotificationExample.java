package com.example.appdev.utils;

import android.content.Context;
import android.view.View;

/**
 * Example class demonstrating how to use the improved CustomNotification system
 * This is just for reference and can be deleted after implementation
 */
public class NotificationExample {

    /**
     * Shows examples of all notification types
     * @param context The context
     * @param view The view to attach click listeners to (optional)
     */
    public static void showExamples(Context context, View view) {
        if (view != null) {
            view.setOnClickListener(v -> {
                // Show a success notification
                CustomNotification.showStatusNotification(
                        context,
                        "Your message was sent successfully!",
                        CustomNotification.NotificationType.SUCCESS
                );
            });
        }
        
        // Examples of different notification types:
        
        // 1. Success notification
        CustomNotification.showStatusNotification(
                context,
                "Profile updated successfully!",
                CustomNotification.NotificationType.SUCCESS
        );
        
        // 2. Error notification
        CustomNotification.showStatusNotification(
                context,
                "Failed to connect to the server. Please try again.",
                CustomNotification.NotificationType.ERROR
        );
        
        // 3. Warning notification
        CustomNotification.showStatusNotification(
                context,
                "Your storage is almost full. Consider deleting some files.",
                CustomNotification.NotificationType.WARNING
        );
        
        // 4. Info notification
        CustomNotification.showStatusNotification(
                context,
                "New update available! Check the settings to update.",
                CustomNotification.NotificationType.INFO
        );
        
        // Using the legacy method (still supported)
        CustomNotification.showNotification(
                context,
                "This is a legacy notification",
                true // isSuccess
        );
    }
}
