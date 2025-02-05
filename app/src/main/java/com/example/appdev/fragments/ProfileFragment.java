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

import com.bumptech.glide.Glide;
import com.example.appdev.UpgradeAccountActivity;
import com.example.appdev.models.User;
import com.example.appdev.subcontrollers.ChangePassControl;
import com.example.appdev.LoginActivity;
import com.example.appdev.R;
import com.example.appdev.Variables;
import com.example.appdev.subcontrollers.ChangeProfilePicControl;
import com.example.appdev.subcontrollers.ChangeLanguageControl;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.appdev.utils.CustomNotification;
import com.example.appdev.subcontrollers.ChangeUsernameControl;
import com.google.android.material.button.MaterialButton;
import androidx.core.content.ContextCompat;
import com.example.appdev.translators.TranslatorType;

public class ProfileFragment extends Fragment {

    private TextView textViewUsername, textViewEmail, textViewTranslatorValue, textViewLanguageValue;
    private ImageView imageViewUserPicture;
    private CardView layoutChangeUsername, layoutProfile, layoutLanguageSelection, layoutChangePass, layoutChangeTranslator;
    private Button btnLogout, btnSaveChanges, btnChangePassword2, btnChangeTranslator, btnUpgrade;
    private ImageButton btnBack;
    private EditText editTextUsername, editTextOldPassword, editTextNewPassword, editTextConfirmPassword;
    private ChangeProfilePicControl changeProfilePicControl;
    private ChangeLanguageControl changeLanguageControl;
    private String accountType;
    private LinearLayout btnMenuSelectTranslator;
    private LinearLayout btnMenuChangeLanguage;
    private LinearLayout btnMenuChangePassword;
    private ChangeUsernameControl changeUsernameControl;
    private ViewGroup translatorButtonsContainer;

    public CardView getLayoutLanguageSelection() {

        return layoutLanguageSelection;
    }

    public CardView getLayoutProfile() {

        return layoutProfile;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Initialize views
        initializeViews(view);
        
        // Setup translator buttons
        translatorButtonsContainer = view.findViewById(R.id.translatorButtonsContainer);
        setupTranslatorButtons(translatorButtonsContainer);
        
        if (Variables.guestUser.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            view.findViewById(R.id.cardViewProfile).setVisibility(View.GONE);
            view.findViewById(R.id.imageViewUserPicture).setVisibility(View.GONE);
            view.findViewById(R.id.headerBackground).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        changeProfilePicControl = new ChangeProfilePicControl(this);
        userDataListener();
        setListeners();


    }

    private void initializeViews(View view) {
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        imageViewUserPicture = view.findViewById(R.id.imageViewUserPicture);
        layoutChangeUsername = view.findViewById(R.id.includeChangeUsername);
        layoutProfile = view.findViewById(R.id.cardViewProfile);
        layoutLanguageSelection = view.findViewById(R.id.includeLanguageSelection);
        layoutChangePass = view.findViewById(R.id.includeChangePassword);
        layoutChangeTranslator = view.findViewById(R.id.includeTranslatorSelection);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnChangePassword2 = view.findViewById(R.id.btnChangePassword2);
        btnMenuSelectTranslator = view.findViewById(R.id.btnMenuSelectTranslator);
        btnBack = view.findViewById(R.id.btnBack);
        btnUpgrade = view.findViewById(R.id.btnUpgrade);
        editTextUsername = view.findViewById(R.id.editTextUsername);
        editTextOldPassword = view.findViewById(R.id.editTextOldPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);
        changeLanguageControl = new ChangeLanguageControl(this);
        textViewTranslatorValue = view.findViewById(R.id.textViewTranslatorValue);
        textViewLanguageValue = view.findViewById(R.id.textViewLanguageValue);
        btnMenuChangeLanguage = view.findViewById(R.id.btnMenuChangeLanguage);
        btnMenuChangePassword = view.findViewById(R.id.btnMenuChangePassword);
        
        // Initialize the controller
        changeUsernameControl = new ChangeUsernameControl(this,
            layoutChangeUsername.findViewById(R.id.editTextUsername),
            layoutChangeUsername,
            layoutProfile);
    }


    //This method automatically updates the values of the user's profile UI when the user data changes in the database
    private void userDataListener(){

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user != null) {
                            textViewLanguageValue.setText(user.getLanguage());
                            textViewUsername.setText(user.getUsername());
                            editTextUsername.setText(user.getUsername());
                            textViewEmail.setText(user.getEmail());
                            accountType = user.getAccountType();
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().equals("none")) {
                                Glide.with(requireContext()).load(user.getProfileImageUrl()).into(imageViewUserPicture);
                            } else {
                                imageViewUserPicture.setImageResource(R.drawable.default_userpic);
                            }
                            if (accountType.equals("free")) {
                                textViewTranslatorValue.setText("Google Translate");
                                btnUpgrade.setVisibility(View.VISIBLE);
                                userRef.child("translator").setValue("google");
                            }
                            else {
                                textViewTranslatorValue.setText(
                                    TranslatorType.fromId(user.getTranslator()).getDisplayName()
                                );
                            }
                            if(accountType.equals("premium")){
                                btnUpgrade.setVisibility(View.GONE);
                            }

                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error getting user: " + databaseError.getMessage());
                }
            });
        }
    }

    // method to set listeners for the buttons and other elements in the profile fragment
    private void setListeners(){

        //LOGOUT LISTENER
        btnLogout.setOnClickListener(v -> logout());

        //CHANGE PROFILE PIC LISTENER
        imageViewUserPicture.setOnClickListener(v ->
                changeProfilePicControl.selectImage()
        );


        //CHANGE LANGUAGE LISTENERS
        btnMenuChangeLanguage.setOnClickListener(v -> toggleCardViews(layoutLanguageSelection, layoutProfile));

        //CHANGE PASSWORD LISTENERS
        btnMenuChangePassword.setOnClickListener(v -> toggleCardViews(layoutChangePass, layoutProfile));
        btnChangePassword2.setOnClickListener(new ChangePassControl(getContext(), 
            editTextOldPassword, 
            editTextNewPassword,
            editTextConfirmPassword, 
            layoutChangePass, 
            layoutProfile,
            this));
        
        // Add back button listener for change password
        View changePassView = layoutChangePass.findViewById(R.id.btnBack);
        changePassView.setOnClickListener(v -> toggleCardViews(layoutProfile, layoutChangePass));

        //CHANGE USERNAME LISTENERS
        textViewUsername.setOnClickListener(v -> toggleCardViews(layoutChangeUsername, layoutProfile));
        
        View usernameBackBtn = layoutChangeUsername.findViewById(R.id.btnBack);
        usernameBackBtn.setOnClickListener(v -> toggleCardViews(layoutProfile, layoutChangeUsername));
        
        layoutChangeUsername.findViewById(R.id.btnSaveChanges)
            .setOnClickListener(v -> changeUsernameControl.updateUsername());

        //CHANGE TRANSLATOR LISTENERS
        btnMenuSelectTranslator.setOnClickListener(v -> {
            if(accountType.equals("free")){
                startActivity(new Intent(getActivity(), UpgradeAccountActivity.class));
                return;
            }
            toggleCardViews(layoutChangeTranslator, layoutProfile);
        });

        // Add back button listener for translator selection
        View translatorView = layoutChangeTranslator.findViewById(R.id.btnBack);
        translatorView.setOnClickListener(v -> toggleCardViews(layoutProfile, layoutChangeTranslator));

        //UPGRADE ACCOUNT LISTENER
        btnUpgrade.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UpgradeAccountActivity.class));

        });

        // Add back button listener for language selection
        View languageBackBtn = layoutLanguageSelection.findViewById(R.id.btnBack);
        languageBackBtn.setOnClickListener(v -> toggleCardViews(layoutProfile, layoutLanguageSelection));

    }
    public void toggleCardViews(CardView cardViewToShow, CardView cardViewToHide) {
        cardViewToHide.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    cardViewToHide.setVisibility(View.GONE);
                    cardViewToShow.setAlpha(0f);
                    cardViewToShow.setVisibility(View.VISIBLE);
                    cardViewToShow.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                }).start();
    }

    private void updateUsername() {
        String newUsername = editTextUsername.getText().toString().trim();
        if (newUsername.isEmpty()) {
            editTextUsername.setError("Username cannot be empty");
            return;
        }

        btnSaveChanges.setEnabled(false);
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("username").setValue(newUsername)
                .addOnSuccessListener(aVoid -> {
                    CustomNotification.showNotification(requireActivity(), 
                        "Username updated successfully", true);
                    toggleCardViews(layoutProfile, layoutChangeUsername);
                })
                .addOnFailureListener(e -> {
                    CustomNotification.showNotification(requireActivity(), 
                        "Failed to update username", false);
                    btnSaveChanges.setEnabled(true);
                });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
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

    private void updateTranslator(String translatorType, String displayName) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("translator").setValue(translatorType)
                .addOnSuccessListener(aVoid -> {
                    CustomNotification.showNotification(requireActivity(), 
                        "Switched to " + displayName, true);
                    toggleCardViews(layoutProfile, layoutChangeTranslator);
                })
                .addOnFailureListener(e -> {
                    CustomNotification.showNotification(requireActivity(), 
                        "Failed to change translator", false);
                });
    }

    private void setupTranslatorButtons(ViewGroup container) {
        for (TranslatorType type : TranslatorType.values()) {
            MaterialButton button = (MaterialButton) LayoutInflater.from(getContext())
                .inflate(R.layout.translator_button, container, false);
            
            button.setText(type.getDisplayName());
            button.setIcon(ContextCompat.getDrawable(requireContext(), type.getIconResourceId()));
            button.setOnClickListener(v -> updateTranslator(type.getId(), type.getDisplayName()));
            
            container.addView(button);
        }
    }
}