package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;

public class BaseAuthActivity extends AppCompatActivity {
    
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