package com.example.appdev.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.appdev.ConnectChatActivity;
import com.example.appdev.R;
import com.example.appdev.models.ConnectionRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class ConnectionRequestManager {
    private static final String TAG = "ConnectionRequestManager";
    private static ConnectionRequestManager instance;
    private DatabaseReference connectionRequestsRef;
    private ValueEventListener connectionRequestsListener;
    private ValueEventListener sentRequestsListener;
    private java.util.Set<String> handledRequestIds;
    private long lastHandledTimestamp;
    private String activeSessionId;

    private ConnectionRequestManager() {
        connectionRequestsRef = FirebaseDatabase.getInstance().getReference("connection_requests");
        handledRequestIds = new java.util.HashSet<>();
        lastHandledTimestamp = System.currentTimeMillis();
    }

    public static synchronized ConnectionRequestManager getInstance() {
        if (instance == null) {
            instance = new ConnectionRequestManager();
        }
        return instance;
    }

    /**
     * Creates a new connection request from current user to target user
     */
    public void createConnectionRequest(String toUserId, String toUserName, String toUserLanguage,
                                      String toUserProfileImageUrl, ConnectionRequestCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            callback.onError("User not authenticated");
            return;
        }

        // Don't allow connecting to yourself
        if (currentUserId.equals(toUserId)) {
            callback.onError("Cannot connect to yourself");
            return;
        }

        // Generate unique IDs
        String requestId = UUID.randomUUID().toString();
        String sessionId = generateSessionId(currentUserId, toUserId);

        // Get current user's info
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance()
                .getReference("users").child(currentUserId);

        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fromUserName = snapshot.child("username").getValue(String.class);
                String fromUserLanguage = snapshot.child("language").getValue(String.class);
                String fromUserProfileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                if (fromUserName == null) fromUserName = "Unknown User";

                // Create connection request
                ConnectionRequest request = new ConnectionRequest(
                    requestId, currentUserId, toUserId, sessionId,
                    fromUserName, fromUserLanguage, fromUserProfileImageUrl
                );

                // Save to Firebase
                connectionRequestsRef.child(requestId).setValue(request)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Connection request created: " + requestId);
                        callback.onSuccess(request);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create connection request", e);
                        callback.onError("Failed to create connection request");
                    });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Failed to get user information");
            }
        });
    }

    /**
     * Start listening for connection requests for the current user
     */
    public void startListeningForRequests(Context context) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) return;

        connectionRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ConnectionRequest request = snapshot.getValue(ConnectionRequest.class);
                    if (request != null && request.getToUserId().equals(currentUserId) && request.isPending()) {
                        showConnectionRequestDialog(context, request, snapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error listening for connection requests", databaseError.toException());
            }
        };

        // Listen for requests addressed to current user
        connectionRequestsRef.orderByChild("toUserId").equalTo(currentUserId)
                .addValueEventListener(connectionRequestsListener);

        // Also listen for requests sent by current user that get accepted
        sentRequestsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ConnectionRequest request = snapshot.getValue(ConnectionRequest.class);
                    if (request != null && request.isAccepted() && request.getFromUserId().equals(currentUserId)) {
                        // Check if we've already handled this request to prevent duplicates
                        if (!handledRequestIds.contains(request.getRequestId())) {
                            // Check if the request was accepted recently (within last 30 seconds)
                            // to avoid re-triggering old accepted requests when app restarts
                            long timeSinceAcceptance = System.currentTimeMillis() - request.getTimestamp();
                            if (timeSinceAcceptance < 30000) { // 30 seconds window
                                // Mark as handled and process
                                handledRequestIds.add(request.getRequestId());
                                Log.d(TAG, "Connection request accepted by recipient: " + request.getRequestId());
                                handleAcceptedRequest(context, request);
                            } else {
                                Log.d(TAG, "Skipping old accepted request: " + request.getRequestId() +
                                      " (accepted " + (timeSinceAcceptance/1000) + " seconds ago)");
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error listening for sent connection requests", databaseError.toException());
            }
        };

        connectionRequestsRef.orderByChild("fromUserId").equalTo(currentUserId)
                .addValueEventListener(sentRequestsListener);
    }

    /**
     * Handle when a connection request sent by current user is accepted
     */
    private void handleAcceptedRequest(Context context, ConnectionRequest request) {
        // Check if we already have an active session for this request
        if (request.getSessionId() != null && request.getSessionId().equals(activeSessionId)) {
            Log.d(TAG, "Session already active: " + request.getSessionId());
            return;
        }

        // Get recipient's information
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(request.getToUserId());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String recipientName = snapshot.child("username").getValue(String.class);
                String recipientLanguage = snapshot.child("language").getValue(String.class);
                String recipientProfileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                if (recipientName == null) recipientName = "Unknown User";

                // Open ConnectChatActivity
                android.content.Intent intent = new android.content.Intent(context, ConnectChatActivity.class);
                intent.putExtra("userId", request.getToUserId());
                intent.putExtra("username", recipientName);
                intent.putExtra("recipientLanguage", recipientLanguage);
                intent.putExtra("profileImageUrl", recipientProfileImageUrl);
                intent.putExtra("sessionId", request.getSessionId());
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(intent);

                // Mark this session as active
                activeSessionId = request.getSessionId();

                Log.d(TAG, "Opened ConnectChatActivity for accepted request: " + request.getRequestId());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error getting recipient info for accepted request", error.toException());
            }
        });
    }

    /**
     * Set the active session ID
     */
    public void setActiveSessionId(String sessionId) {
        this.activeSessionId = sessionId;
    }

    /**
     * Clear the active session ID
     */
    public void clearActiveSessionId() {
        this.activeSessionId = null;
    }

    /**
     * Stop listening for connection requests
     */
    public void stopListeningForRequests() {
        if (connectionRequestsListener != null && connectionRequestsRef != null) {
            connectionRequestsRef.removeEventListener(connectionRequestsListener);
            connectionRequestsListener = null;
        }

        if (sentRequestsListener != null && connectionRequestsRef != null) {
            connectionRequestsRef.removeEventListener(sentRequestsListener);
            sentRequestsListener = null;
        }
    }

    /**
     * Reset tracking state - call this when app starts fresh
     */
    public void resetTrackingState() {
        handledRequestIds.clear();
        lastHandledTimestamp = System.currentTimeMillis();
        activeSessionId = null;
        Log.d(TAG, "Reset tracking state for fresh app session");
    }

    /**
     * Cancel a connection request (called by the requesting user)
     */
    public void cancelConnectionRequest(String requestId) {
        DatabaseReference requestRef = connectionRequestsRef.child(requestId);
        requestRef.child("status").setValue("CANCELLED")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Connection request cancelled: " + requestId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to cancel connection request: " + requestId, e);
                });
    }

    /**
     * Accept a connection request
     */
    public void acceptConnectionRequest(String requestId, ConnectionRequest request) {
        connectionRequestsRef.child(requestId).child("status").setValue("ACCEPTED")
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Connection request accepted: " + requestId);
                // The listener will handle opening the ConnectChatActivity
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to accept connection request", e);
            });
    }

    /**
     * Reject a connection request
     */
    public void rejectConnectionRequest(String requestId) {
        connectionRequestsRef.child(requestId).child("status").setValue("REJECTED")
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Connection request rejected: " + requestId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to reject connection request", e);
            });
    }

    private void showConnectionRequestDialog(Context context, ConnectionRequest request, String requestId) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.connection_request_dialog);

        // Set dialog properties
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                           android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Initialize views
        de.hdodenhof.circleimageview.CircleImageView profileImage = dialog.findViewById(R.id.profileImage);
        android.widget.TextView userNameText = dialog.findViewById(R.id.userName);
        android.widget.TextView userLanguageText = dialog.findViewById(R.id.userLanguage);
        androidx.appcompat.widget.AppCompatButton acceptButton = dialog.findViewById(R.id.acceptButton);
        androidx.appcompat.widget.AppCompatButton rejectButton = dialog.findViewById(R.id.rejectButton);
        android.widget.ImageButton closeButton = dialog.findViewById(R.id.closeButton);

        // Set user information
        userNameText.setText(request.getFromUserName());
        userLanguageText.setText("Language: " + request.getFromUserLanguage());

        // Load profile image
        if (request.getFromUserProfileImageUrl() != null && !request.getFromUserProfileImageUrl().equals("none")) {
            Glide.with(context)
                    .load(request.getFromUserProfileImageUrl())
                    .placeholder(R.drawable.default_userpic)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.default_userpic);
        }

        // Listen for status changes (including cancellation)
        DatabaseReference requestRef = connectionRequestsRef.child(requestId);
        ValueEventListener statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) return;

                String currentStatus = dataSnapshot.child("status").getValue(String.class);
                if (currentStatus != null && currentStatus.equals("CANCELLED")) {
                    // Request was cancelled by the sender
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    handler.post(() -> {
                        // Update UI to show cancelled status
                        userLanguageText.setText("Request cancelled");
                        userLanguageText.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));

                        // Disable buttons
                        acceptButton.setEnabled(false);
                        rejectButton.setEnabled(false);
                        acceptButton.setText("Request Cancelled");
                        rejectButton.setVisibility(android.view.View.GONE);

                        // Auto-close after 3 seconds
                        handler.postDelayed(() -> {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }, 3000);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error listening for request status changes", databaseError.toException());
            }
        };

        requestRef.addValueEventListener(statusListener);

        // Set click listeners
        acceptButton.setOnClickListener(v -> {
            requestRef.removeEventListener(statusListener);
            dialog.dismiss();
            acceptConnectionRequest(requestId, request);
            // Open ConnectChatActivity
            android.content.Intent intent = new android.content.Intent(context, ConnectChatActivity.class);
            intent.putExtra("userId", request.getFromUserId());
            intent.putExtra("username", request.getFromUserName());
            intent.putExtra("recipientLanguage", request.getFromUserLanguage());
            intent.putExtra("profileImageUrl", request.getFromUserProfileImageUrl());
            intent.putExtra("sessionId", request.getSessionId());
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });

        rejectButton.setOnClickListener(v -> {
            requestRef.removeEventListener(statusListener);
            dialog.dismiss();
            rejectConnectionRequest(requestId);
        });

        closeButton.setOnClickListener(v -> {
            requestRef.removeEventListener(statusListener);
            dialog.dismiss();
            rejectConnectionRequest(requestId);
        });

        dialog.show();
    }

    private String generateSessionId(String userId1, String userId2) {
        String[] ids = {userId1, userId2};
        java.util.Arrays.sort(ids);
        return ids[0] + "_" + ids[1];
    }

    public interface ConnectionRequestCallback {
        void onSuccess(ConnectionRequest request);
        void onError(String error);
    }
}
