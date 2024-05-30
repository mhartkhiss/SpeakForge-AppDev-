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

public class ProfileFragment extends Fragment {

    private TextView textViewUsername, textViewEmail;
    private ImageView imageViewUserPicture;
    private CardView layoutChangeUsername, layoutProfile, layoutLanguageSelection, layoutChangePass, layoutChangeTranslator;
    private Button btnLogout, btnSaveChanges, btnChangeLanguage, btnChangePassword, btnChangePassword2, btnChangeTranslator,
            btnGoogle, btnOpenAi, btnBack, btnUpgrade;
    private Button[] btnLanguages = new Button[3];
    private EditText editTextUsername, editTextOldPassword, editTextNewPassword, editTextConfirmPassword;
    private ChangeProfilePicControl changeProfilePicControl;
    private ChangeLanguageControl changeLanguageControl;
    private String accountType;

    public CardView getLayoutLanguageSelection() {

        return layoutLanguageSelection;
    }

    public CardView getLayoutProfile() {

        return layoutProfile;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initializeViews(view);
        if (Variables.guestUser.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {
            view.setVisibility(View.GONE);
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
        layoutChangeUsername = view.findViewById(R.id.cardViewChangeUsername);
        layoutProfile = view.findViewById(R.id.cardViewProfile);
        layoutLanguageSelection = view.findViewById(R.id.includeLanguageSelection);
        layoutChangePass = view.findViewById(R.id.includeChangePassword);
        layoutChangeTranslator = view.findViewById(R.id.includeTranslatorSelection);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnChangeLanguage = view.findViewById(R.id.btnMenuChangeLanguage);
        btnChangePassword = view.findViewById(R.id.btnMenuChangePassword);
        btnChangePassword2 = view.findViewById(R.id.btnChangePassword2);
        btnChangeTranslator = view.findViewById(R.id.btnMenuSelectTranslator);
        btnGoogle = view.findViewById(R.id.btnGoogle);
        btnOpenAi = view.findViewById(R.id.btnOpenAi);
        btnBack = view.findViewById(R.id.btnBack);
        btnUpgrade = view.findViewById(R.id.btnUpgrade);
        btnLanguages[0] = view.findViewById(R.id.btnBisaya);
        btnLanguages[1] = view.findViewById(R.id.btnTagalog);
        btnLanguages[2] = view.findViewById(R.id.btnEnglish);
        editTextUsername = view.findViewById(R.id.editTextUsername);
        editTextOldPassword = view.findViewById(R.id.editTextOldPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);
        changeLanguageControl = new ChangeLanguageControl(this);

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
                            btnChangeLanguage.setText("Language: " + user.getLanguage());
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
                                btnChangeTranslator.setText("Translator: Google Translate");
                                btnUpgrade.setVisibility(View.VISIBLE);
                                userRef.child("translator").setValue("google");
                            }
                            else if (user.getTranslator().equals("openai")) {
                                btnChangeTranslator.setText("Translator: OpenAI");
                            } else {
                                btnChangeTranslator.setText("Translator: Google Translate");
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
        btnChangeLanguage.setOnClickListener(v ->
                toggleCardViews(layoutLanguageSelection, layoutProfile)
        );
        //LANGUAGE BUTTONS LISTENERS
        for (Button btnLanguage : btnLanguages) {
            btnLanguage.setOnClickListener(v -> changeLanguageControl.updateUserLanguage(btnLanguage.getText().toString()));
        }

        //CHANGE PASSWORD LISTENERS
        btnChangePassword.setOnClickListener(v -> toggleCardViews(layoutChangePass, layoutProfile));
        btnChangePassword2.setOnClickListener(new ChangePassControl(getContext(), editTextOldPassword, editTextNewPassword,
                editTextConfirmPassword, layoutChangePass, layoutProfile));
        btnBack.setOnClickListener(v -> toggleCardViews(layoutProfile, layoutChangePass));

        //CHANGE USERNAME LISTENERS
        textViewUsername.setOnClickListener(v -> toggleCardViews(layoutChangeUsername, layoutProfile));
        btnSaveChanges.setOnClickListener(v -> updateUsername());

        //CHANGE TRANSLATOR LISTENERS
        btnChangeTranslator.setOnClickListener(v -> {
            if(accountType.equals("free")){
                startActivity(new Intent(getActivity(), UpgradeAccountActivity.class));
                return;
            }
            toggleCardViews(layoutChangeTranslator, layoutProfile);
        });
        btnGoogle.setOnClickListener(v -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            userRef.child("translator").setValue("google");
            toggleCardViews(layoutProfile, layoutChangeTranslator);
        });
        btnOpenAi.setOnClickListener(v -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            userRef.child("translator").setValue("openai");
            toggleCardViews(layoutProfile, layoutChangeTranslator);


        });

        //UPGRADE ACCOUNT LISTENER
        btnUpgrade.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), UpgradeAccountActivity.class));

        });

    }
    private void toggleCardViews(CardView cardViewToShow, CardView cardViewToHide) {
        cardViewToShow.setVisibility(View.VISIBLE);
        cardViewToHide.setVisibility(View.GONE);
    }

    private void updateUsername() {
        String newUsername = editTextUsername.getText().toString().trim();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        userRef.child("username").setValue(newUsername)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                    toggleCardViews(layoutProfile, layoutChangeUsername);
                })
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to update username", Toast.LENGTH_SHORT).show());
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
}