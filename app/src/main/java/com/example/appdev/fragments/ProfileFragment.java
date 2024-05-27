package com.example.appdev.fragments;

import static android.content.ContentValues.TAG;

import android.content.Intent;
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
import com.example.appdev.helpers.FirebaseHelper;
import com.example.appdev.models.User;
import com.example.appdev.subcontrollers.ChangePassControl;
import com.example.appdev.LoginActivity;
import com.example.appdev.R;
import com.example.appdev.helpers.FetchUserField;
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
    private CardView layoutChangeUsername, layoutProfile, layoutLanguageSelection, layoutChangePass, layoutChangeTranslator,
            layoutApiKey;
    private Button btnSaveChanges, btnChangeLanguage, btnChangePassword, btnChangePassword2, btnChangeTranslator, btnSetApiKey,
            btnGoogle, btnOpenAi, btnBack, btnBack2, btnSaveApiKey;
    private Button[] btnLanguages = new Button[3];
    private EditText editTextUsername, editTextOldPassword, editTextNewPassword, editTextConfirmPassword, editTextApiKey;
    private ChangeProfilePicControl changeProfilePicControl;
    private ChangeLanguageControl changeLanguageControl;

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

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
        FirebaseHelper.getCurrentUser(new FirebaseHelper.UserCallback() {
            @Override
            public void onUserReceived(User user) {
                btnChangeLanguage.setText("Language: "+user.getLanguage());
                editTextApiKey.setText(user.getApiKey());
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error getting user: " + errorMessage);
            }
        });

        changeProfilePicControl = new ChangeProfilePicControl(this);

        setUserDetails();
        setListeners();
        translatorMenu();


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
        layoutApiKey = view.findViewById(R.id.includeSetApiKey);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnChangeLanguage = view.findViewById(R.id.btnMenuChangeLanguage);
        btnChangePassword = view.findViewById(R.id.btnMenuChangePassword);
        btnChangePassword2 = view.findViewById(R.id.btnChangePassword2);
        btnChangeTranslator = view.findViewById(R.id.btnMenuSelectTranslator);
        btnGoogle = view.findViewById(R.id.btnGoogle);
        btnOpenAi = view.findViewById(R.id.btnOpenAi);
        btnSetApiKey = view.findViewById(R.id.btnMenuSetApiKey);
        btnBack = view.findViewById(R.id.btnBack);
        btnBack2 = view.findViewById(R.id.btnBack2);
        btnSaveApiKey = view.findViewById(R.id.btnSaveApiKey);
        btnLanguages[0] = view.findViewById(R.id.btnBisaya);
        btnLanguages[1] = view.findViewById(R.id.btnTagalog);
        btnLanguages[2] = view.findViewById(R.id.btnEnglish);
        editTextUsername = view.findViewById(R.id.editTextUsername);
        editTextOldPassword = view.findViewById(R.id.editTextOldPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);
        editTextApiKey = view.findViewById(R.id.editTextApiKey);
        changeLanguageControl = new ChangeLanguageControl(this);

    }



    private void translatorMenu() {

        FirebaseHelper.getCurrentUser(new FirebaseHelper.UserCallback() {
            @Override
            public void onUserReceived(User user) {
                if(user.getTranslator().equals("openai")){
                    btnChangeTranslator.setText("Translator: OpenAI");
                    if(user.getAccountType().equals("free")){
                        btnSetApiKey.setVisibility(View.VISIBLE);
                    }
                    else{
                        btnSetApiKey.setVisibility(View.GONE);
                    }
                }
                else{
                    btnChangeTranslator.setText("Translator: Google Translate");
                    btnSetApiKey.setVisibility(View.GONE);
                }
            }
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error getting user: " + errorMessage);
            }
        });
    }

    private void setListeners(){

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
            btnLanguage.setOnClickListener(v -> changeLanguageControl.updateUserLanguage(btnLanguage.getText().toString(), btnChangeLanguage));
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
        btnChangeTranslator.setOnClickListener(v -> toggleCardViews(layoutChangeTranslator, layoutProfile));
        btnGoogle.setOnClickListener(v -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            userRef.child("translator").setValue("google");
            btnChangeTranslator.setText("Translator: Google Translate");
            translatorMenu();
            toggleCardViews(layoutProfile, layoutChangeTranslator);
        });
        btnOpenAi.setOnClickListener(v -> {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            userRef.child("translator").setValue("openai");
            btnChangeTranslator.setText("Translator: OpenAI");
            translatorMenu();
            toggleCardViews(layoutProfile, layoutChangeTranslator);
        });

        //SET API KEY LISTENERS
        btnSetApiKey.setOnClickListener(v -> {
                toggleCardViews(layoutApiKey, layoutProfile);

        });
        btnBack2.setOnClickListener(v -> toggleCardViews(layoutProfile, layoutApiKey));
        btnSaveApiKey.setOnClickListener(v -> {


            EditText editTextApiKey = requireView().findViewById(R.id.editTextApiKey);
            String apiKey = editTextApiKey.getText().toString().trim();

            if (!apiKey.isEmpty()) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                userRef.child("apiKey").setValue(apiKey);
                Toast.makeText(getActivity(), "API Key updated successfully", Toast.LENGTH_SHORT).show();
                toggleCardViews(layoutProfile, layoutApiKey);
            } else {
                Toast.makeText(getActivity(), "Please enter an API Key", Toast.LENGTH_SHORT).show();
            }

        });

    }
    private void toggleCardViews(CardView cardViewToShow, CardView cardViewToHide) {
        cardViewToShow.setVisibility(View.VISIBLE);
        cardViewToHide.setVisibility(View.GONE);
    }

    private void updateUsername() {
        String newUsername = editTextUsername.getText().toString().trim();
        if (!newUsername.isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

                userRef.child("username").setValue(newUsername)
                        .addOnSuccessListener(aVoid -> {
                            textViewUsername.setText(newUsername);
                            Toast.makeText(getActivity(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Failed to update username", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to update username: " + e.getMessage());
                        });
            }
        } else {
            Toast.makeText(getActivity(), "Please enter a username", Toast.LENGTH_SHORT).show();
        }

        toggleCardViews(layoutProfile, layoutChangeUsername);
    }

    private void setUserDetails() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            textViewEmail.setText(currentUser.getEmail());
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String username = dataSnapshot.child("username").getValue(String.class);
                        String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                        if (username != null && !username.isEmpty()) {
                            textViewUsername.setText(username);
                            editTextUsername.setText(username);
                        }
                        if (profileImageUrl != null && !profileImageUrl.equals("none")) {
                            Glide.with(requireContext()).load(profileImageUrl).into(imageViewUserPicture);
                        } else {
                            imageViewUserPicture.setImageResource(R.drawable.default_userpic);
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
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