package com.example.appdev;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.viewpager.widget.ViewPager;

import android.widget.ImageView;
import android.widget.TextView;

import com.example.appdev.adapter.TabAdapter;
import com.example.appdev.fragments.ChatFragment;
import com.example.appdev.fragments.ProfileFragment;
import com.example.appdev.fragments.VoiceFragment;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        // Create an adapter that returns a fragment for each tab
        TabAdapter adapter = new TabAdapter(getSupportFragmentManager());
        adapter.addFragment(new ProfileFragment(), "Profile");
        adapter.addFragment(new VoiceFragment(), "Voice");
        adapter.addFragment(new ChatFragment(), "Chat");

        // Set the adapter onto the view pager
        viewPager.setAdapter(adapter);

        // Connect the tab layout with the view pager
        tabLayout.setupWithViewPager(viewPager);

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
                        tabIcon.setImageResource(R.drawable.ic_voice);
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
