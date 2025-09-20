package com.example.appdev.fragments;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appdev.QRScanActivity;
import com.example.appdev.UpgradeAccountActivity;
import com.example.appdev.models.User;
import com.example.appdev.subcontrollers.ChangePassControl;
import com.example.appdev.LoginActivity;
import com.example.appdev.R;
import com.example.appdev.Variables;
import com.example.appdev.subcontrollers.ChangeProfilePicControl;
import com.example.appdev.subcontrollers.ChangeLanguageControl;
import com.example.appdev.subcontrollers.ChangeUsernameControl;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.appdev.utils.CustomNotification;
import com.google.android.material.button.MaterialButton;
import androidx.core.content.ContextCompat;
import com.example.appdev.translators.TranslatorType;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.appdev.adapters.LanguageAdapter;
import java.util.HashSet;
import java.util.Set;
import com.example.appdev.subcontrollers.ChangeTranslatorControl;

public class ProfileFragment extends Fragment {

    private TextView textViewUsername, textViewEmail, textViewTranslatorValue, textViewLanguageValue;
    private ImageView imageViewUserPicture;
    private CardView layoutProfile;
    private Button btnLogout;
    private ImageButton btnBack;
    private ChangeProfilePicControl changeProfilePicControl;
    private ChangeLanguageControl changeLanguageControl;
    private String accountType;
    private LinearLayout btnMenuSelectTranslator;
    private LinearLayout btnMenuChangeLanguage;
    private LinearLayout btnMenuChangePassword;
    private LinearLayout btnMenuShareQR;
    private LinearLayout btnMenuTranslationMode;
    private ImageButton btnTranslationModeToggle;
    private TextView textViewTranslationModeValue;
    private ChangeUsernameControl changeUsernameControl;
    private ViewGroup translatorButtonsContainer;
    private TextView friendsCountView;
    private TextView userTypeView;
    private ValueEventListener valueEventListener;
    private ValueEventListener friendsCountListener;
    private ChangeTranslatorControl changeTranslatorControl;

    public CardView getLayoutProfile() {
        return layoutProfile;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Initialize views
        initializeViews(view);
        
        // Setup translator buttons using the controller
        translatorButtonsContainer = view.findViewById(R.id.translatorButtonsContainer);
        changeTranslatorControl = new ChangeTranslatorControl(this);
        changeTranslatorControl.setupTranslatorButtons(translatorButtonsContainer);
        
        // Check if this is a guest user
        if ("guest".equals(Variables.userUID)) {
            
            // Hide all UI elements in guest mode
            // Hide the main components
            view.findViewById(R.id.cardViewProfile).setVisibility(View.GONE);
            view.findViewById(R.id.imageViewUserPicture).setVisibility(View.GONE);
            view.findViewById(R.id.headerBackground).setVisibility(View.GONE);
            
            // Hide all other UI components
            if (view.findViewById(R.id.textViewUsername) != null)
                view.findViewById(R.id.textViewUsername).setVisibility(View.GONE);
            if (view.findViewById(R.id.textViewEmail) != null)
                view.findViewById(R.id.textViewEmail).setVisibility(View.GONE);
            if (view.findViewById(R.id.btnLogout) != null)
                view.findViewById(R.id.btnLogout).setVisibility(View.GONE);
            // Keep back button visible for all users (they need to navigate back)
            if (view.findViewById(R.id.textViewMemberSince) != null)
                view.findViewById(R.id.textViewMemberSince).setVisibility(View.GONE);
            if (view.findViewById(R.id.textViewFriendsCount) != null)
                view.findViewById(R.id.textViewFriendsCount).setVisibility(View.GONE);
            if (view.findViewById(R.id.textViewUserType) != null)
                view.findViewById(R.id.textViewUserType).setVisibility(View.GONE);
                
            // Hide the menu items
            if (view.findViewById(R.id.btnMenuChangeLanguage) != null)
                view.findViewById(R.id.btnMenuChangeLanguage).setVisibility(View.GONE);
            if (view.findViewById(R.id.btnMenuChangePassword) != null)
                view.findViewById(R.id.btnMenuChangePassword).setVisibility(View.GONE);
            if (view.findViewById(R.id.btnMenuSelectTranslator) != null)
                view.findViewById(R.id.btnMenuSelectTranslator).setVisibility(View.GONE);
                
            // Hide the stats section
            if (view.findViewById(R.id.cardViewStats) != null)
                view.findViewById(R.id.cardViewStats).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Skip loading user data in guest mode
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && "guest".equals(Variables.userUID)) {
            return; // Skip the rest of initialization in guest mode
        }
        
        changeProfilePicControl = new ChangeProfilePicControl(this);
        userDataListener();
        setListeners();

        // Initialize stats views
        friendsCountView = view.findViewById(R.id.textViewFriendsCount);
        userTypeView = view.findViewById(R.id.textViewUserType);
        TextView memberSinceView = view.findViewById(R.id.textViewMemberSince);

        // Update friends count based on conversation rooms
        updateFriendsCount();
        
        // Member since will be updated in userDataListener
    }

    private void initializeViews(View view) {
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        imageViewUserPicture = view.findViewById(R.id.imageViewUserPicture);
        
        layoutProfile = view.findViewById(R.id.cardViewProfile);
        
        btnLogout = view.findViewById(R.id.btnLogout);
        btnBack = view.findViewById(R.id.btnBack);
        
        changeLanguageControl = new ChangeLanguageControl(this);
        textViewTranslatorValue = view.findViewById(R.id.textViewTranslatorValue);
        textViewLanguageValue = view.findViewById(R.id.textViewLanguageValue);
        
        // Initialize menu buttons
        btnMenuChangeLanguage = view.findViewById(R.id.btnMenuChangeLanguage);
        btnMenuChangePassword = view.findViewById(R.id.btnMenuChangePassword);
        btnMenuSelectTranslator = view.findViewById(R.id.btnMenuSelectTranslator);
        btnMenuShareQR = view.findViewById(R.id.btnMenuShareQR);
        btnMenuTranslationMode = view.findViewById(R.id.btnMenuTranslationMode);
        btnTranslationModeToggle = view.findViewById(R.id.btnTranslationModeToggle);
        textViewTranslationModeValue = view.findViewById(R.id.textViewTranslationModeValue);

        changeUsernameControl = new ChangeUsernameControl(this, null, null, layoutProfile);

        // Initialize translation mode UI
        updateTranslationModeUI();

        // Add user type layout initialization
        View layoutUserType = view.findViewById(R.id.layoutUserType);
        layoutUserType.setOnClickListener(v -> {
            if (accountType.equals("free")) {
                startActivity(new Intent(getActivity(), UpgradeAccountActivity.class));
            }
        });
    }


    //This method automatically updates the values of the user's profile UI when the user data changes in the database
    private void userDataListener(){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || "guest".equals(Variables.userUID)) {
            // Skip for guest users
            return;
        }
        
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if fragment is still attached
                if (!isAdded()) {
                    return;
                }

                if (dataSnapshot.exists()) {
                    User user = dataSnapshot.getValue(User.class);
                    if (user != null) {
                        textViewLanguageValue.setText(user.getLanguage());
                        textViewUsername.setText(user.getUsername());
                        textViewEmail.setText(user.getEmail());
                        accountType = user.getAccountType();
                        
                        // Update member since view with abbreviated month format
                        TextView memberSinceView = getView().findViewById(R.id.textViewMemberSince);
                        if (user.getCreatedAt() != null) {
                            try {
                                // Parse the date string to create a Date object
                                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM d, yyyy");
                                java.util.Date date = inputFormat.parse(user.getCreatedAt());
                                String formattedDate = outputFormat.format(date);
                                memberSinceView.setText(formattedDate);
                            } catch (Exception e) {
                                Log.e(TAG, "Error formatting date: " + e.getMessage());
                                memberSinceView.setText(user.getCreatedAt());
                            }
                        }
                        
                        // Wrap Glide operations in isAdded() check
                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none") && isAdded()) {
                            Glide.with(getContext())
                                .load(user.getProfileImageUrl())
                                .into(imageViewUserPicture);
                        } else {
                            imageViewUserPicture.setImageResource(R.drawable.default_userpic);
                        }
                        
                        if (accountType.equals("free")) {
                            textViewTranslatorValue.setText("Google Translate");
                            userRef.child("translator").setValue("google");
                        } else {
                            textViewTranslatorValue.setText(
                                TranslatorType.fromId(user.getTranslator()).getDisplayName()
                            );
                        }
                        // Update user type text based on account type
                        userTypeView.setText(accountType.equals("premium") ? "Premium" : "Free User");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (isAdded()) {
                    Log.e(TAG, "Error getting user: " + databaseError.getMessage());
                }
            }
        };
        userRef.addValueEventListener(valueEventListener);
    }

    // method to set listeners for the buttons and other elements in the profile fragment
    private void setListeners(){

        //BACK BUTTON LISTENER
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // Go back to the main translation screen
                if (getActivity() != null) {
                    // Check if there's a back stack entry and pop it
                    if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        // Fallback: directly replace with BasicTranslationFragment
                        getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.mainContentFrame, new BasicTranslationFragment())
                            .commit();
                    }
                }
            });
            // Make sure button is visible
            btnBack.setVisibility(View.VISIBLE);
        }

        //LOGOUT LISTENER
        btnLogout.setOnClickListener(v -> logout());

        //CHANGE PROFILE PIC LISTENER
        imageViewUserPicture.setOnClickListener(v ->
                changeProfilePicControl.selectImage()
        );


        //CHANGE LANGUAGE LISTENERS
        if (btnMenuChangeLanguage != null) {
            btnMenuChangeLanguage.setOnClickListener(v -> 
                showBottomSheetDialog(R.layout.fragment_profile_sub_changelanguage, "Select Language"));
        }

        //CHANGE PASSWORD LISTENERS
        if (btnMenuChangePassword != null) {
            btnMenuChangePassword.setOnClickListener(v -> 
                showBottomSheetDialog(R.layout.fragment_profile_sub_changepass, "Change Password"));
        }

        if (textViewUsername != null) {
            textViewUsername.setOnClickListener(v -> 
                showBottomSheetDialog(R.layout.fragment_profile_sub_changeusername, "Change Username"));
        }
        
        if (btnMenuSelectTranslator != null) {
            btnMenuSelectTranslator.setOnClickListener(v -> {
                if(accountType.equals("free")){
                    startActivity(new Intent(getActivity(), UpgradeAccountActivity.class));
                    return;
                }
                showBottomSheetDialog(R.layout.fragment_profile_sub_changetranslator, "Select Translator");
            });
        }

        if (btnMenuShareQR != null) {
            btnMenuShareQR.setOnClickListener(v -> showQRCodeDialog());
        }

        // Translation mode toggle
        if (btnMenuTranslationMode != null) {
            btnMenuTranslationMode.setOnClickListener(v -> toggleTranslationMode());
        }
        if (btnTranslationModeToggle != null) {
            btnTranslationModeToggle.setOnClickListener(v -> toggleTranslationMode());
        }

    }

    private void showBottomSheetDialog(int layoutResId, String title) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.ModalBottomSheetDialog);
        View bottomSheetView = getLayoutInflater().inflate(layoutResId, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Setup back button
        ImageButton btnBack = bottomSheetView.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        // Handle specific layout setup
        switch (layoutResId) {
            case R.layout.fragment_profile_sub_changepass:
                setupChangePasswordDialog(bottomSheetView, bottomSheetDialog);
                break;
            case R.layout.fragment_profile_sub_changelanguage:
                setupLanguageDialog(bottomSheetView, bottomSheetDialog);
                break;
            case R.layout.fragment_profile_sub_changetranslator:
                setupTranslatorDialog(bottomSheetView, bottomSheetDialog);
                break;
            case R.layout.fragment_profile_sub_changeusername:
                setupUsernameDialog(bottomSheetView, bottomSheetDialog);
                break;
        }

        bottomSheetDialog.show();
    }

    private void setupLanguageDialog(View view, BottomSheetDialog dialog) {
        changeLanguageControl.setupLanguageDialog(view, dialog);
    }

    private void setupTranslatorDialog(View view, BottomSheetDialog dialog) {
        ViewGroup container = view.findViewById(R.id.translatorButtonsContainer);
        if (container != null) {
            changeTranslatorControl.setupTranslatorButtons(container, dialog);
        }
    }

    private void setupChangePasswordDialog(View view, BottomSheetDialog dialog) {
        EditText oldPassword = view.findViewById(R.id.editTextOldPassword);
        EditText newPassword = view.findViewById(R.id.editTextNewPassword);
        EditText confirmPassword = view.findViewById(R.id.editTextConfirmPassword);
        Button btnChangePassword = view.findViewById(R.id.btnChangePassword2);

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangePassControl passControl = new ChangePassControl(requireContext(),
                    oldPassword, newPassword, confirmPassword, null, null, ProfileFragment.this);
                passControl.onClick(v);
                dialog.dismiss();
            }
        });
    }

    private void setupUsernameDialog(View view, BottomSheetDialog dialog) {
        changeUsernameControl.setupUsernameDialog(view, dialog, textViewUsername.getText().toString());
    }

    private void logout() {
        // Clear all user-related variables before signing out
        Variables.userUID = "";
        Variables.userEmail = "";
        Variables.userDisplayName = "";
        Variables.userAccountType = "";
        Variables.userLanguage = "";
        Variables.userTranslator = "";
        Variables.roomId = "";
        
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Navigate to login screen
        startActivity(new Intent(requireActivity(), LoginActivity.class));
        requireActivity().finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        changeProfilePicControl.onActivityResult(requestCode, resultCode, data);
    }

    public void updateUserProfilePicture(String imageUrl) {
        Glide.with(requireContext()).load(imageUrl).into(imageViewUserPicture);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Remove the ValueEventListener when the view is destroyed
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && valueEventListener != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid());
            userRef.removeEventListener(valueEventListener);
        }
        
        // Remove the friends count listener
        if (currentUser != null && friendsCountListener != null) {
            DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages");
            messagesRef.removeEventListener(friendsCountListener);
        }
    }

    private void updateFriendsCount() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || "guest".equals(Variables.userUID)) {
            // Skip for guest users
            return;
        }
        
        String currentUserId = currentUser.getUid();
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        
        friendsCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> uniqueContacts = new HashSet<>();
                
                for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                    String roomId = roomSnapshot.getKey();
                    if (roomId != null && roomId.contains(currentUserId)) {
                        // Extract the other user's ID from the room ID
                        String otherUserId = roomId.replace(currentUserId + "_", "")
                                                 .replace("_" + currentUserId, "");
                        uniqueContacts.add(otherUserId);
                    }
                }
                
                // Update the friends count view
                if (friendsCountView != null) {
                    friendsCountView.setText(String.valueOf(uniqueContacts.size()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ProfileFragment", "Error getting conversation rooms: " + databaseError.getMessage());
            }
        };
        
        messagesRef.addValueEventListener(friendsCountListener);
    }

    private void showQRCodeDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || "guest".equals(Variables.userUID)) {
            CustomNotification.showNotification(requireContext(),
                    "You need to be logged in to share QR code", false);
            return;
        }

        // Create a bottom sheet dialog for QR code display
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(
                R.layout.qr_code_share_dialog, null);

        // Initialize views
        ImageView qrCodeImage = bottomSheetView.findViewById(R.id.qrCodeImage);
        TextView userNameText = bottomSheetView.findViewById(R.id.userNameText);
        ImageButton closeButton = bottomSheetView.findViewById(R.id.closeButton);

        // Set user name
        userNameText.setText(Variables.userDisplayName);

        // Generate QR code
        com.example.appdev.utils.QRCodeGenerator qrGenerator = new com.example.appdev.utils.QRCodeGenerator();
        android.graphics.Bitmap qrBitmap = qrGenerator.generateUserQRCode(
                Variables.userUID,
                Variables.userDisplayName,
                Variables.userLanguage,
                null); // Profile image URL can be added later

        if (qrBitmap != null) {
            qrCodeImage.setImageBitmap(qrBitmap);
        } else {
            CustomNotification.showNotification(requireContext(),
                    "Failed to generate QR code", false);
            return;
        }

        // Set close button listener
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void toggleTranslationMode() {
        // Toggle the translation mode
        Variables.isFormalTranslationMode = !Variables.isFormalTranslationMode;

        // Save to SharedPreferences
        com.example.appdev.utils.TranslationModeManager.saveToPreferences(requireContext(), Variables.isFormalTranslationMode);

        // Update the UI
        updateTranslationModeUI();

        // Show feedback
        String modeText = Variables.isFormalTranslationMode ? "Formal" : "Casual";
        Toast.makeText(requireContext(), "Translation mode set to " + modeText, Toast.LENGTH_SHORT).show();
    }

    private void updateTranslationModeUI() {
        if (btnTranslationModeToggle != null && textViewTranslationModeValue != null) {
            if (Variables.isFormalTranslationMode) {
                btnTranslationModeToggle.setImageResource(R.drawable.translation_mode_formal);
                textViewTranslationModeValue.setText("Formal");
            } else {
                btnTranslationModeToggle.setImageResource(R.drawable.translation_mode_casual);
                textViewTranslationModeValue.setText("Casual");
            }
        }
    }
}