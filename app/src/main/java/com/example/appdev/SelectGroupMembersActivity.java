package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.appdev.adapters.SelectMembersAdapter;
import com.example.appdev.models.User;
import com.example.appdev.utils.CustomNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SelectGroupMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerViewContacts;
    private EditText editTextSearch;
    private ProgressBar progressBar;
    private TextView textViewNoContacts;
    private Button buttonDone;
    private ImageView imageViewBack;
    
    private SelectMembersAdapter adapter;
    private List<User> allContacts;
    private List<User> filteredContacts;
    private HashMap<String, Boolean> selectedMembers;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group_members);
        
        // Get selected members from intent if available
        if (getIntent() != null && getIntent().getSerializableExtra("selectedMembers") != null) {
            selectedMembers = (HashMap<String, Boolean>) getIntent().getSerializableExtra("selectedMembers");
        } else {
            selectedMembers = new HashMap<>();
        }
        
        // Initialize views
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        editTextSearch = findViewById(R.id.editTextSearch);
        progressBar = findViewById(R.id.progressBar);
        textViewNoContacts = findViewById(R.id.textViewNoContacts);
        buttonDone = findViewById(R.id.buttonDone);
        imageViewBack = findViewById(R.id.imageViewBack);
        
        // Setup RecyclerView
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        allContacts = new ArrayList<>();
        filteredContacts = new ArrayList<>();
        
        adapter = new SelectMembersAdapter(this, filteredContacts, selectedMembers);
        recyclerViewContacts.setAdapter(adapter);
        
        // Setup search functionality
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString().toLowerCase().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Setup back button
        imageViewBack.setOnClickListener(v -> finish());
        
        // Setup done button
        buttonDone.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedMembers", selectedMembers);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        
        // Load contacts
        loadContacts();
    }
    
    private void loadContacts() {
        progressBar.setVisibility(View.VISIBLE);
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allContacts.clear();
                
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    User user = dataSnapshot.getValue(User.class);
                    String userId = dataSnapshot.getKey();
                    
                    // Skip current user and already selected members
                    if (user != null && userId != null && !userId.equals(currentUserId)) {
                        user.setUserId(userId);
                        allContacts.add(user);
                    }
                }
                
                progressBar.setVisibility(View.GONE);
                
                if (allContacts.isEmpty()) {
                    textViewNoContacts.setVisibility(View.VISIBLE);
                } else {
                    textViewNoContacts.setVisibility(View.GONE);
                    filteredContacts.addAll(allContacts);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                textViewNoContacts.setVisibility(View.VISIBLE);
                CustomNotification.showNotification(SelectGroupMembersActivity.this, 
                    "Failed to load contacts: " + error.getMessage(), false);
            }
        });
    }
    
    private void filterContacts(String query) {
        filteredContacts.clear();
        
        if (query.isEmpty()) {
            filteredContacts.addAll(allContacts);
        } else {
            for (User user : allContacts) {
                if (user.getUsername() != null && user.getUsername().toLowerCase().contains(query) ||
                    user.getEmail() != null && user.getEmail().toLowerCase().contains(query)) {
                    filteredContacts.add(user);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        
        if (filteredContacts.isEmpty()) {
            textViewNoContacts.setText("No contacts found");
            textViewNoContacts.setVisibility(View.VISIBLE);
        } else {
            textViewNoContacts.setVisibility(View.GONE);
        }
    }
}
