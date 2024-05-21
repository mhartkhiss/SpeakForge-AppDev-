package com.example.appdev.tasks;

import androidx.annotation.NonNull;

import com.example.appdev.Variables;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class FetchUserField {

    public interface UserFieldListener {
        void onFieldReceived(String fieldValue);
        void onError(DatabaseError databaseError);
    }

    public static void fetchUserField(String field, UserFieldListener listener) {
        if (Variables.currentUser != null) {
            Variables.userRef.child(field).addListenerForSingleValueEvent(new ValueEventListener() {
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