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
import com.example.appdev.controller.ChangePassControl;
import com.example.appdev.LoginActivity;
import com.example.appdev.R;
import com.example.appdev.classes.FetchUserField;
import com.example.appdev.classes.Variables;
import com.example.appdev.controller.ChangeProfilePicControl;
import com.example.appdev.controller.UserLanguageControl;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
    private CardView cardViewEditDetails, cardViewProfile, cardViewLanguage, cardViewChangePassword;
    private Button btnSaveChanges, btnChangeLanguage, btnChangePassword, btnChangePassword2, btnEnglish, btnTagalog, btnBisaya;
    private Button[] btnLanguages = new Button[3];
    private EditText editTextUsername, editTextOldPassword, editTextNewPassword, editTextConfirmPassword;
    private ChangeProfilePicControl changeProfilePicControl;
    private UserLanguageControl userLanguageControl;

    public CardView getCardViewLanguage() {
        return cardViewLanguage;
    }

    public CardView getCardViewProfile() {
        return cardViewProfile;
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
        FetchUserField.fetchUserField("language", new FetchUserField.UserFieldListener() {
            @Override
            public void onFieldReceived(String fieldValue) {
                btnChangeLanguage.setText("Language: "+fieldValue);
            }
            @Override
            public void onError(DatabaseError databaseError) {
            }
        });

        changeProfilePicControl = new ChangeProfilePicControl(this);

        setUserDetails();
        setListeners();


    }

    private void initializeViews(View view) {
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewEmail = view.findViewById(R.id.textViewEmail);
        imageViewUserPicture = view.findViewById(R.id.imageViewUserPicture);
        cardViewEditDetails = view.findViewById(R.id.cardViewEditDetails);
        cardViewProfile = view.findViewById(R.id.cardViewProfile);
        cardViewLanguage = view.findViewById(R.id.cardViewLanguage);
        cardViewChangePassword = view.findViewById(R.id.cardViewChangePassword);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnChangeLanguage = view.findViewById(R.id.btnChangeLanguage);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnChangePassword2 = view.findViewById(R.id.btnChangePassword2);
        btnLanguages[0] = view.findViewById(R.id.btnBisaya);
        btnLanguages[1] = view.findViewById(R.id.btnTagalog);
        btnLanguages[2] = view.findViewById(R.id.btnEnglish);
        editTextUsername = view.findViewById(R.id.editTextUsername);
        editTextOldPassword = view.findViewById(R.id.editTextOldPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);
        userLanguageControl = new UserLanguageControl(this);
    }

    private void setListeners(){

        //CHANGE PROFILE PIC LISTENER
        imageViewUserPicture.setOnClickListener(v ->
                changeProfilePicControl.selectImage()
        );


        //CHANGE LANGUAGE LISTENERS
        btnChangeLanguage.setOnClickListener(v ->
                toggleCardViews(cardViewLanguage, cardViewProfile)
        );
        //LANGUAGE BUTTONS LISTENERS
        for (Button btnLanguage : btnLanguages) {
            btnLanguage.setOnClickListener(v -> userLanguageControl.updateUserLanguage(btnLanguage.getText().toString(), btnChangeLanguage));
        }

        //CHANGE PASSWORD LISTENERS
        btnChangePassword.setOnClickListener(v -> toggleCardViews(cardViewChangePassword, cardViewProfile));
        btnChangePassword2.setOnClickListener(new ChangePassControl(getContext(), editTextOldPassword, editTextNewPassword,
                editTextConfirmPassword, cardViewChangePassword, cardViewProfile));


        //CHANGE USERNAME LISTENERS
        textViewUsername.setOnClickListener(v -> toggleCardViews(cardViewEditDetails, cardViewProfile));
        btnSaveChanges.setOnClickListener(v -> saveChanges());

    }
    private void toggleCardViews(CardView cardViewToShow, CardView cardViewToHide) {
        cardViewToShow.setVisibility(View.VISIBLE);
        cardViewToHide.setVisibility(View.GONE);
    }

    private void saveChanges() {
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

        toggleCardViews(cardViewProfile, cardViewEditDetails);
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