package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Rect;
import android.view.ViewTreeObserver;
import android.view.View;
import android.widget.ImageView;
import android.util.TypedValue;
import android.os.Build;

public class BaseAuthActivity extends AppCompatActivity {
    
    protected void setupKeyboardVisibilityListener(ImageView logo) {
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean wasOpened;
            private final int defaultKeyboardHeightDP = 100;
            private final int EstimatedKeyboardDP = defaultKeyboardHeightDP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0);
            private final Rect r = new Rect();

            @Override
            public void onGlobalLayout() {
                // Convert the dp to pixels.
                int estimatedKeyboardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EstimatedKeyboardDP, rootView.getResources().getDisplayMetrics());

                // Conclude whether the keyboard is shown or not.
                rootView.getWindowVisibleDisplayFrame(r);
                int heightDiff = rootView.getRootView().getHeight() - (r.bottom - r.top);
                boolean isShown = heightDiff >= estimatedKeyboardHeight;

                if (isShown != wasOpened) {
                    wasOpened = isShown;
                    logo.setVisibility(isShown ? View.GONE : View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in_activity, R.anim.fade_out_activity);
    }
} 