package com.example.appdev.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appdev.R;

public class CustomNotification {
    
    public static void showNotification(Context context, String message, boolean isSuccess) {
        // Inflate the custom layout
        View layout = LayoutInflater.from(context).inflate(R.layout.custom_notification, null);

        // Get views
        ImageView icon = layout.findViewById(R.id.notificationIcon);
        TextView text = layout.findViewById(R.id.notificationText);

        // Set icon and text
        icon.setImageResource(isSuccess ? R.drawable.ic_success : R.drawable.ic_error);
        text.setText(message);

        // Create and show toast
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}