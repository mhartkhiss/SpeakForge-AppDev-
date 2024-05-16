package com.example.appdev;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.TextView;

public class ProgressDialog {
    Activity activity;
    AlertDialog dialog;

    public ProgressDialog(Activity activity) {
        this.activity = activity;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(R.layout.progress);
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }

    public void setText(String text) {
        TextView textView = dialog.findViewById(R.id.progressText);
        textView.setText(text);
    }

    public void dismiss() {
        dialog.dismiss();
    }
}
