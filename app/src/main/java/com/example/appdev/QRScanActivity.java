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
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appdev.utils.CustomNotification;
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
                // Launch ChatActivity with the scanned user info
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("userId", userId);
                intent.putExtra("username", username);
                intent.putExtra("recipientLanguage", language);
                intent.putExtra("profileImageUrl", profileImageUrl);
                startActivity(intent);
                finish(); // Close the scanning activity
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
