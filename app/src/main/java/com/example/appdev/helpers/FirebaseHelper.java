package com.example.appdev.helpers;

import androidx.annotation.NonNull;
import com.example.appdev.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseHelper {

    public interface UserCallback {
        void onUserReceived(User user);
        void onError(String errorMessage);
    }

    public static void getCurrentUser(UserCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        callback.onUserReceived(user);
                    } else {
                        callback.onError("User data is stale.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
        } else {
            callback.onError("Permission denied.");
        }
    }
}
/*Sample usage:

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
 */