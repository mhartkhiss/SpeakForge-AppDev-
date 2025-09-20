package com.example.appdev.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeGenerator {

    /**
     * Generates a QR code bitmap for user profile sharing
     * @param userId The user's unique ID
     * @param username The user's display name
     * @param language The user's language
     * @param profileImageUrl The user's profile image URL (can be null)
     * @param width The width of the QR code bitmap
     * @param height The height of the QR code bitmap
     * @return Bitmap containing the QR code, or null if generation fails
     */
    public static Bitmap generateUserQRCode(String userId, String username, String language,
                                          String profileImageUrl, int width, int height) {
        try {
            // Create the data string for the QR code
            StringBuilder qrData = new StringBuilder();
            qrData.append("speakforge://user?");
            qrData.append("userId=").append(userId);
            qrData.append("&username=").append(java.net.URLEncoder.encode(username, "UTF-8"));
            qrData.append("&language=").append(language);

            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                qrData.append("&profileImageUrl=").append(java.net.URLEncoder.encode(profileImageUrl, "UTF-8"));
            } else {
                qrData.append("&profileImageUrl=none");
            }

            // Generate QR code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData.toString(), BarcodeFormat.QR_CODE, width, height);

            // Convert BitMatrix to Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;

        } catch (WriterException | java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a QR code bitmap with default dimensions (300x300)
     * @param userId The user's unique ID
     * @param username The user's display name
     * @param language The user's language
     * @param profileImageUrl The user's profile image URL (can be null)
     * @return Bitmap containing the QR code, or null if generation fails
     */
    public static Bitmap generateUserQRCode(String userId, String username, String language,
                                          String profileImageUrl) {
        return generateUserQRCode(userId, username, language, profileImageUrl, 300, 300);
    }
}
