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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.appdev.adapters.LanguageAdapter;
import java.util.HashSet;
import java.util.Set;

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
    private ChangeUsernameControl changeUsernameControl;
    private ViewGroup translatorButtonsContainer;
    private TextView friendsCountView;
    private TextView userTypeView;
    private ValueEventListener valueEventListener;

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

        changeUsernameControl = new ChangeUsernameControl(this, null, null, layoutProfile);

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
        if (currentUser != null) {
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
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewLanguages);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            LanguageAdapter adapter = new LanguageAdapter(requireContext(), language -> {
                changeLanguageControl.updateUserLanguage(language);
                dialog.dismiss();
            });
            recyclerView.setAdapter(adapter);
        }
    }

    private void setupTranslatorDialog(View view, BottomSheetDialog dialog) {
        ViewGroup container = view.findViewById(R.id.translatorButtonsContainer);
        if (container != null) {
            for (TranslatorType type : TranslatorType.values()) {
                MaterialButton button = (MaterialButton) LayoutInflater.from(getContext())
                    .inflate(R.layout.translator_button, container, false);
                
                button.setText(type.getDisplayName());
                button.setIcon(ContextCompat.getDrawable(requireContext(), type.getIconResourceId()));
                button.setOnClickListener(v -> {
                    updateTranslator(type.getId(), type.getDisplayName());
                    dialog.dismiss();
                });
                
                container.addView(button);
            }
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
        EditText usernameInput = view.findViewById(R.id.editTextUsername);
        Button btnSave = view.findViewById(R.id.btnSaveChanges);
        usernameInput.setText(textViewUsername.getText());

        btnSave.setOnClickListener(v -> {
            String newUsername = usernameInput.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                updateUsername(newUsername, dialog);
            }
        });
    }

    private void updateUsername(String newUsername, BottomSheetDialog dialog) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("username").setValue(newUsername)
                .addOnSuccessListener(aVoid -> {
                    CustomNotification.showNotification(requireActivity(), 
                        "Username updated successfully", true);
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    CustomNotification.showNotification(requireActivity(), 
                        "Failed to update username", false);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the ValueEventListener when the view is destroyed
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUser.getUid());
            userRef.removeEventListener(valueEventListener);
        }
    }

    private void updateFriendsCount() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages");
        
        messagesRef.addValueEventListener(new ValueEventListener() {
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
        });
    }
}