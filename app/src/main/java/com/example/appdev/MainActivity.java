package com.example.appdev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.appdev.adapter.TabAdapter;
import com.example.appdev.classes.Variables;
import com.example.appdev.fragments.ChatFragment;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.fragments.VoiceFragment;
import com.example.appdev.models.User;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    public static User currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child("userId");

        // Add ValueEventListener to retrieve user data
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Check if user data exists
                if (dataSnapshot.exists()) {
                    // Get user data
                    String userId = dataSnapshot.child("userId").getValue(String.class);
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);
                    String sourceLanguage = dataSnapshot.child("sourceLanguage").getValue(String.class);
                    String targetLanguage = dataSnapshot.child("targetLanguage").getValue(String.class);

                    // Create User object
                    //currentUser = new User(userId, username, email, profileImageUrl);
                    //currentUser.setUserLanguage(sourceLanguage);
                    currentUser.setTargetLanguage(targetLanguage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
                Log.e("MainActivity", "Error fetching user data: " + databaseError.getMessage());
            }
        });


        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        // Create an adapter that returns a fragment for each tab
        TabAdapter adapter = new TabAdapter(getSupportFragmentManager());
        adapter.addFragment(new ProfileFragment(), "Profile");
        adapter.addFragment(new VoiceFragment(), "");
        adapter.addFragment(new ChatFragment(), "Chat");

        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);

        // Connect the tab layout with the view pager
        tabLayout.setupWithViewPager(viewPager);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

                // Check if the selected tab is the profile or chat tab and user's email is "a@gmail.com"
                if ((position == 0 || position == 2) && userEmail.equals(Variables.guestUser)) {

                    // Display a dialog with message and options to login or cancel
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Login Required")
                            .setMessage("You need to login to use this feature. Do you want to login now?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Sign out the current user and proceed to LoginActivity
                                    mAuth.signOut();
                                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                    finish(); // Finish the current activity to prevent returning to it
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Dismiss the dialog if user chooses not to login
                                    dialog.dismiss();
                                    tabLayout.getTabAt(1).select(); // Select the voice tab
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    // Redirect the user to the voice tab when the dialog is canceled
                                    tabLayout.getTabAt(1).select(); // Select the voice tab
                                }
                            })
                            .create()
                            .show();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Unused method
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Unused method
            }
        });


        // Add custom tab items with icons
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                tab.setCustomView(R.layout.custom_tab_item);
                TextView tabText = tab.getCustomView().findViewById(R.id.tabText);
                ImageView tabIcon = tab.getCustomView().findViewById(R.id.tabIcon);
                // Set your tab title and icon here
                tabText.setText(adapter.getPageTitle(i));
                switch (i) {
                    case 0:
                        tabIcon.setImageResource(R.drawable.ic_profile);
                        break;
                    case 1:
                        tabIcon.setImageResource(R.drawable.ic_translate);
                        break;
                    case 2:
                        tabIcon.setImageResource(R.drawable.ic_chat);
                        break;
                }

            }
        }
        // Select the "Voice" tab as the default tab
        TabLayout.Tab defaultTab = tabLayout.getTabAt(1); // Index of the "Voice" tab
        if (defaultTab != null) {
            defaultTab.select();
        }
    }

}
