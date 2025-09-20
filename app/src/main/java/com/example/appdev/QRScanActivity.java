package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Dialog;

import com.bumptech.glide.Glide;
import com.example.appdev.ConnectChatActivity;
import com.example.appdev.utils.ConnectionRequestManager;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScanActivity extends AppCompatActivity {

    private static final String TAG = "QRScanActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private PreviewView previewView;
    private ImageButton buttonBack;
    private TextView textViewScanning;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private ProcessCameraProvider cameraProvider;
    private boolean isScanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        // Initialize views
        previewView = findViewById(R.id.previewView);
        buttonBack = findViewById(R.id.buttonBack);
        textViewScanning = findViewById(R.id.textViewScanning);

        // Set up back button
        buttonBack.setOnClickListener(v -> finish());

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize barcode scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image analysis for QR code scanning
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (!isScanning) {
                        image.close();
                        return;
                    }

                    processImage(image);
                });

                // Select back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    Camera camera = cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageAnalysis);

                } catch (Exception exc) {
                    Log.e(TAG, "Use case binding failed", exc);
                    CustomNotification.showNotification(this,
                            "Failed to start camera", false);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera provider initialization failed", e);
                CustomNotification.showNotification(this,
                        "Failed to initialize camera", false);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImage(@NonNull androidx.camera.core.ImageProxy imageProxy) {
        InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(),
                imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && rawValue.startsWith("speakforge://")) {
                            // Stop scanning to prevent multiple detections
                            isScanning = false;

                            // Parse the QR code data
                            parseQRCodeData(rawValue);
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Barcode scanning failed", e);
                })
                .addOnCompleteListener(task -> {
                    imageProxy.close();
                });
    }

    private void parseQRCodeData(String qrData) {
        try {
            // Expected format: speakforge://user?userId=USER_ID&username=USERNAME&language=LANGUAGE&profileImageUrl=URL
            String data = qrData.replace("speakforge://user?", "");
            String[] params = data.split("&");

            String userId = null;
            String username = null;
            String language = null;
            String profileImageUrl = null;

            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];

                    switch (key) {
                        case "userId":
                            userId = value;
                            break;
                        case "username":
                            username = java.net.URLDecoder.decode(value, "UTF-8");
                            break;
                        case "language":
                            language = value;
                            break;
                        case "profileImageUrl":
                            profileImageUrl = value.equals("none") ? null :
                                    java.net.URLDecoder.decode(value, "UTF-8");
                            break;
                    }
                }
            }

            if (userId != null && username != null && language != null) {
                // Immediately send connection request and show waiting dialog
                sendConnectionRequestAndShowWaiting(userId, username, language, profileImageUrl);
            } else {
                // Invalid QR code format
                runOnUiThread(() -> {
                    CustomNotification.showNotification(this,
                            "Invalid QR code format", false);
                    isScanning = true; // Resume scanning
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing QR code data", e);
            runOnUiThread(() -> {
                CustomNotification.showNotification(this,
                        "Failed to parse QR code", false);
                isScanning = true; // Resume scanning
            });
        }
    }

    private void sendConnectionRequestAndShowWaiting(String userId, String username, String language, String profileImageUrl) {
        // Fetch additional user details from Firebase
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String fullUsername = dataSnapshot.child("username").getValue(String.class);
                    String userLanguage = dataSnapshot.child("language").getValue(String.class);
                    String userProfileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    // Show the waiting dialog and send request
                    showWaitingDialog(userId, fullUsername != null ? fullUsername : username,
                                    userLanguage != null ? userLanguage : language,
                                    userProfileImageUrl != null ? userProfileImageUrl : profileImageUrl);
                } else {
                    // User not found, show basic info
                    showWaitingDialog(userId, username, language, profileImageUrl);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error fetching user details", databaseError.toException());
                // Show basic info on error
                showWaitingDialog(userId, username, language, profileImageUrl);
            }
        });
    }

    private void showWaitingDialog(String userId, String username, String language, String profileImageUrl) {
        runOnUiThread(() -> {
            Dialog waitingDialog = new Dialog(this);
            waitingDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            waitingDialog.setContentView(R.layout.connect_confirmation_dialog);

            // Set dialog properties
            android.view.Window window = waitingDialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                               android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            // Initialize dialog views
            de.hdodenhof.circleimageview.CircleImageView profileImage = waitingDialog.findViewById(R.id.profileImage);
            TextView userNameText = waitingDialog.findViewById(R.id.userName);
            TextView userLanguageText = waitingDialog.findViewById(R.id.userLanguage);
            TextView statusText = waitingDialog.findViewById(R.id.statusText);
            TextView waitingText = waitingDialog.findViewById(R.id.waitingText);
            androidx.appcompat.widget.AppCompatButton cancelButton = waitingDialog.findViewById(R.id.cancelButton);
            ImageButton closeButton = waitingDialog.findViewById(R.id.closeButton);

            // Set user information
            userNameText.setText(username);
            userLanguageText.setText("Language: " + language);
            statusText.setText("Sending request...");
            waitingText.setText("Please wait...");

            // Load profile image
            if (profileImageUrl != null && !profileImageUrl.equals("none")) {
                Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.default_userpic)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.default_userpic);
            }

            // Store requestId for cancellation
            final String[] currentRequestId = {null};

            // Immediately send the connection request
            ConnectionRequestManager.getInstance().createConnectionRequest(
                userId, username, language, profileImageUrl,
                new ConnectionRequestManager.ConnectionRequestCallback() {
                    @Override
                    public void onSuccess(com.example.appdev.models.ConnectionRequest request) {
                        currentRequestId[0] = request.getRequestId();
                        runOnUiThread(() -> {
                            statusText.setText("Request sent successfully!");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            waitingText.setText("Waiting for " + username + " to respond...");

                            // Listen for request status changes
                            listenForRequestStatus(request.getRequestId(), waitingDialog, statusText, waitingText, username);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            statusText.setText("Failed to send request");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            waitingText.setText(error);
                            cancelButton.setText("Close");
                        });
                    }
                });

            // Set click listeners
            cancelButton.setOnClickListener(v -> {
                // Cancel the connection request if it exists
                if (currentRequestId[0] != null) {
                    ConnectionRequestManager.getInstance().cancelConnectionRequest(currentRequestId[0]);
                }
                waitingDialog.dismiss();
                isScanning = true; // Resume scanning
            });

            closeButton.setOnClickListener(v -> {
                // Cancel the connection request if it exists
                if (currentRequestId[0] != null) {
                    ConnectionRequestManager.getInstance().cancelConnectionRequest(currentRequestId[0]);
                }
                waitingDialog.dismiss();
                isScanning = true; // Resume scanning
            });

            waitingDialog.setOnCancelListener(dialog -> {
                isScanning = true; // Resume scanning when dialog is cancelled
            });

            waitingDialog.setCancelable(false); // Prevent back button from dismissing
            waitingDialog.show();
        });
    }

    private void listenForRequestStatus(String requestId, Dialog waitingDialog, TextView statusText, TextView waitingText, String username) {
        DatabaseReference requestRef = FirebaseDatabase.getInstance()
                .getReference("connection_requests").child(requestId);

        requestRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;

                String status = dataSnapshot.child("status").getValue(String.class);
                if (status == null) return;

                runOnUiThread(() -> {
                    switch (status) {
                        case "ACCEPTED":
                            statusText.setText("Request accepted!");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            waitingText.setText("Opening chat...");
                            // Dismiss dialog after showing success message
                            new android.os.Handler().postDelayed(() -> {
                                if (waitingDialog.isShowing()) {
                                    waitingDialog.dismiss();
                                }
                            }, 1500);
                            break;

                        case "REJECTED":
                            statusText.setText("Request rejected");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            waitingText.setText("The connection request was declined.");
                            // Auto-dismiss after showing the message
                            new android.os.Handler().postDelayed(() -> {
                                if (waitingDialog.isShowing()) {
                                    waitingDialog.dismiss();
                                    isScanning = true; // Resume scanning
                                }
                            }, 3000);
                            break;

                        case "TIMEOUT":
                        case "EXPIRED":
                            statusText.setText("Request expired");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                            waitingText.setText("The request has expired.");
                            // Auto-dismiss after showing the message
                            new android.os.Handler().postDelayed(() -> {
                                if (waitingDialog.isShowing()) {
                                    waitingDialog.dismiss();
                                    isScanning = true; // Resume scanning
                                }
                            }, 3000);
                            break;

                        default:
                            // Still pending
                            break;
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error listening for request status", databaseError.toException());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                CustomNotification.showNotification(this,
                        "Camera permission is required to scan QR codes", false);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
