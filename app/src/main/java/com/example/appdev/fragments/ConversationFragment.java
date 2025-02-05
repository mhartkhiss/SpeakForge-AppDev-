package com.example.appdev.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appdev.R;
import com.example.appdev.Variables;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConversationFragment extends Fragment {
    private EditText user1Input, user2Input;
    private TextView user1Translation, user2Translation;
    private TextView user1Language, user2Language;
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        user1Input = view.findViewById(R.id.user1Input);
        user2Input = view.findViewById(R.id.user2Input);
        user1Translation = view.findViewById(R.id.user1Translation);
        user2Translation = view.findViewById(R.id.user2Translation);
        user1Language = view.findViewById(R.id.user1Language);
        user2Language = view.findViewById(R.id.user2Language);

        // Set initial languages
        user1Language.setText(Variables.userLanguage);
        
        // Get current user's language from Firebase
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        setupTranslationListeners();
    }

    private void setupTranslationListeners() {
        // TODO: Implement real-time translation as users type
        user1Input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Translate text for user 2
                String text = s.toString();
                if (!text.isEmpty()) {
                    translateText(text, user2Translation, true);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        user2Input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Translate text for user 1
                String text = s.toString();
                if (!text.isEmpty()) {
                    translateText(text, user1Translation, false);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void translateText(String text, TextView targetView, boolean isUser1) {
        // TODO: Implement translation using your preferred translator
        // For now, just show the text
        targetView.setText("Translation of: " + text);
    }
} 