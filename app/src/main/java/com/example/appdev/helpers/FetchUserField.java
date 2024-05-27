package com.example.appdev.helpers;

import androidx.annotation.NonNull;

import com.example.appdev.Variables;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FetchUserField {

    public interface UserFieldListener {
        void onFieldReceived(String fieldValue);
        void onError(DatabaseError databaseError);
    }

    public static void fetchUserField(String field, UserFieldListener listener) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        if (currentUser != null) {
            userRef.child(field).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String fieldValue = dataSnapshot.getValue(String.class);
                    listener.onFieldReceived(fieldValue);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    listener.onError(databaseError);
                }
            });
        }
    }
}

/* Sample usage:

FetchUserField.fetchUserField("targetLanguage", new FetchUserField.UserFieldListener() {
    @Override
    public void onFieldReceived(String fieldValue) {
        // Use the field value here
    }

    @Override
    public void onError(DatabaseError databaseError) {
        // Handle error
    }
});

*/