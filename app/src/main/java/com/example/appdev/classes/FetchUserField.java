package com.example.appdev.classes;

import androidx.annotation.NonNull;

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
        // Assume currentUser is the logged-in user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users").child(userId);

            // Add a ValueEventListener to listen for changes to the user's field
            userRef.child(field).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Get the field value
                    String fieldValue = dataSnapshot.getValue(String.class);
                    // Notify listener with the field value
                    listener.onFieldReceived(fieldValue);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Notify listener with the error
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