package com.example.appdev.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appdev.R;
import com.example.appdev.adapter.UserAdapter;
import com.example.appdev.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);

        // Initialize RecyclerView
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, requireContext(), FirebaseAuth.getInstance().getCurrentUser().getUid());
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewUsers.setAdapter(userAdapter);

        // Retrieve list of users from Firebase Authentication
        getUsersFromFirebase();
    }

    private void getUsersFromFirebase() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Convert each user data snapshot to a User object
                    User user = snapshot.getValue(User.class);
                    if (user != null && !user.getUserId().equals(currentUserId)) {
                        // Exclude the current user from the list
                        userList.add(user);
                    }
                }
                // Notify the adapter that the data set has changed
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle error
            }
        });
    }
}
