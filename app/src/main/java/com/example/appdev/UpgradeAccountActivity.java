package com.example.appdev;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UpgradeAccountActivity extends AppCompatActivity {

    private Button upgradeButton, cancelButton;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade_account);

        progressDialog = new ProgressDialog(UpgradeAccountActivity.this);
        upgradeButton = findViewById(R.id.upgrade_button);
        upgradeButton.setOnClickListener(v -> upgradeAccount());
        cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());

    }
    private void upgradeAccount() {
        progressDialog.show();
        progressDialog.setText("Processing payment...");
        new Handler().postDelayed(() -> {
            //update the user's account type in the database
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            userRef.child("accountType").setValue("premium");
            progressDialog.dismiss();
            //show a custom alert dialog to the user to inform them that their account has been upgraded
            new AlertDialog.Builder(UpgradeAccountActivity.this)
                    .setTitle("Account Upgraded")
                    .setMessage("Your account has been upgraded to premium.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
        }, 3000); // delay of 3 seconds
    }
}