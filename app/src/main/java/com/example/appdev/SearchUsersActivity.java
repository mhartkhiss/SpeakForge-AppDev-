package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.appdev.adapters.SearchUserAdapter;
import com.example.appdev.models.User;
import com.example.appdev.utils.ConnectionRequestManager;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchUsersActivity extends AppCompatActivity {

    private static final String TAG = "SearchUsersActivity";

    private RecyclerView recyclerViewUsers;
    private EditText editTextSearch;
    private ProgressBar progressBar;
    private TextView textViewNoUsers;
    private ImageView imageViewBack;

    private SearchUserAdapter searchUserAdapter;
    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users);

        // Get current user ID
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        editTextSearch = findViewById(R.id.editTextSearch);
        progressBar = findViewById(R.id.progressBar);
        textViewNoUsers = findViewById(R.id.textViewNoUsers);
        imageViewBack = findViewById(R.id.imageViewBack);

        // Set up back button
        imageViewBack.setOnClickListener(v -> finish());

        // Set up RecyclerView
        searchUserAdapter = new SearchUserAdapter(filteredUsers, this, currentUserId, this::onUserClicked);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(searchUserAdapter);

        // Set up search functionality
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load all users
        loadUsers();
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoUsers.setVisibility(View.GONE);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allUsers.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    // Skip current user
                    if (userId != null && !userId.equals(currentUserId)) {
                        String username = userSnapshot.child("username").getValue(String.class);
                        String email = userSnapshot.child("email").getValue(String.class);
                        String language = userSnapshot.child("language").getValue(String.class);
                        String profileImageUrl = userSnapshot.child("profileImageUrl").getValue(String.class);

                        if (username != null && email != null) {
                            User user = new User();
                            user.setUserId(userId);
                            user.setUsername(username);
                            user.setEmail(email);
                            user.setLanguage(language != null ? language : "Unknown");
                            user.setProfileImageUrl(profileImageUrl);
                            allUsers.add(user);
                        }
                    }
                }

                progressBar.setVisibility(View.GONE);

                if (allUsers.isEmpty()) {
                    textViewNoUsers.setText("No users available");
                    textViewNoUsers.setVisibility(View.VISIBLE);
                } else {
                    textViewNoUsers.setVisibility(View.GONE);
                }

                // Initially show no results until user searches
                filteredUsers.clear();
                searchUserAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading users: " + databaseError.getMessage());
                progressBar.setVisibility(View.GONE);
                textViewNoUsers.setText("Failed to load users");
                textViewNoUsers.setVisibility(View.VISIBLE);
                CustomNotification.showNotification(SearchUsersActivity.this,
                        "Failed to load users", false);
            }
        });
    }

    private void filterUsers(String query) {
        filteredUsers.clear();

        if (query.trim().isEmpty()) {
            // Show no results when search is empty
            textViewNoUsers.setText("Start typing to search users");
            textViewNoUsers.setVisibility(View.VISIBLE);
        } else {
            // Filter users based on username or email
            for (User user : allUsers) {
                if (user.getUsername().toLowerCase().contains(query.toLowerCase()) ||
                    user.getEmail().toLowerCase().contains(query.toLowerCase())) {
                    filteredUsers.add(user);
                }
            }

            if (filteredUsers.isEmpty()) {
                textViewNoUsers.setText("No users found matching \"" + query + "\"");
                textViewNoUsers.setVisibility(View.VISIBLE);
            } else {
                textViewNoUsers.setVisibility(View.GONE);
            }
        }

        searchUserAdapter.notifyDataSetChanged();
    }

    private void onUserClicked(User user) {
        // Show waiting dialog and send connection request (same as QR scan)
        showWaitingDialogAndSendRequest(user);
    }

    private void showWaitingDialogAndSendRequest(User user) {
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
            userNameText.setText(user.getUsername());
            userLanguageText.setText("Language: " + user.getLanguage());
            statusText.setText("Sending request...");
            waitingText.setText("Please wait...");

            // Load profile image
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none")) {
                Glide.with(this)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.default_userpic)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.default_userpic);
            }

            // Store requestId for cancellation
            final String[] currentRequestId = {null};

            // Immediately send the connection request
            ConnectionRequestManager.getInstance().createConnectionRequest(
                user.getUserId(), user.getUsername(), user.getLanguage(), user.getProfileImageUrl(),
                new ConnectionRequestManager.ConnectionRequestCallback() {
                    @Override
                    public void onSuccess(com.example.appdev.models.ConnectionRequest request) {
                        currentRequestId[0] = request.getRequestId();
                        runOnUiThread(() -> {
                            statusText.setText("Request sent successfully!");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            waitingText.setText("Waiting for " + user.getUsername() + " to respond...");

                            // Listen for request status changes
                            listenForRequestStatus(request.getRequestId(), waitingDialog, statusText, waitingText, user.getUsername());
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
            });

            closeButton.setOnClickListener(v -> {
                // Cancel the connection request if it exists
                if (currentRequestId[0] != null) {
                    ConnectionRequestManager.getInstance().cancelConnectionRequest(currentRequestId[0]);
                }
                waitingDialog.dismiss();
            });

            waitingDialog.setOnCancelListener(dialog -> {
                // Cancel the connection request if it exists
                if (currentRequestId[0] != null) {
                    ConnectionRequestManager.getInstance().cancelConnectionRequest(currentRequestId[0]);
                }
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
                            new Handler().postDelayed(() -> {
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
                            new Handler().postDelayed(() -> {
                                if (waitingDialog.isShowing()) {
                                    waitingDialog.dismiss();
                                }
                            }, 3000);
                            break;

                        case "TIMEOUT":
                        case "EXPIRED":
                            statusText.setText("Request expired");
                            statusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                            waitingText.setText("The request has expired.");
                            // Auto-dismiss after showing the message
                            new Handler().postDelayed(() -> {
                                if (waitingDialog.isShowing()) {
                                    waitingDialog.dismiss();
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
}
