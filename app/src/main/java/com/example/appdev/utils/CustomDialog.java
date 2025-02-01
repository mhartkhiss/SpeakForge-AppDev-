package com.example.appdev.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.appdev.R;
import com.google.android.material.button.MaterialButton;

public class CustomDialog extends Dialog {
    private ImageView dialogIcon;
    private TextView dialogTitle, dialogMessage;
    private MaterialButton btnOk;
    private Context context;

    public CustomDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog);

        // Initialize views
        dialogIcon = findViewById(R.id.dialogIcon);
        dialogTitle = findViewById(R.id.dialogTitle);
        dialogMessage = findViewById(R.id.dialogMessage);
        btnOk = findViewById(R.id.btnOk);

        // Set dialog window attributes
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // Set default button click listener
        btnOk.setOnClickListener(v -> dismiss());
    }

    public static void showDialog(Context context, String title, String message) {
        CustomDialog dialog = new CustomDialog(context);
        
        // Set different icons and colors based on the dialog type
        if (title.toLowerCase().contains("welcome") || title.toLowerCase().contains("success")) {
            dialog.setIcon(R.drawable.ic_check_circle);
            dialog.setIconTint(context.getColor(R.color.success_green));
        } else {
            dialog.setIcon(R.drawable.ic_error);
            dialog.setIconTint(context.getColor(R.color.error_red));
        }
        
        dialog.dialogTitle.setText(title);
        dialog.dialogMessage.setText(message);
        dialog.show();
    }

    public static void showDialog(Context context, String title, String message, OnClickListener listener) {
        CustomDialog dialog = new CustomDialog(context);
        
        // Set different icons and colors based on the dialog type
        if (title.toLowerCase().contains("welcome") || title.toLowerCase().contains("success")) {
            dialog.setIcon(R.drawable.ic_check_circle);
            dialog.setIconTint(context.getColor(R.color.success_green));
        } else {
            dialog.setIcon(R.drawable.ic_error);
            dialog.setIconTint(context.getColor(R.color.error_red));
        }
        
        dialog.dialogTitle.setText(title);
        dialog.dialogMessage.setText(message);
        dialog.btnOk.setOnClickListener(v -> {
            listener.onClick(dialog, BUTTON_POSITIVE);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void setIcon(int resourceId) {
        dialogIcon.setImageResource(resourceId);
    }

    private void setIconTint(int color) {
        dialogIcon.setColorFilter(color);
    }

    public void setButtonText(String text) {
        btnOk.setText(text);
    }
} 