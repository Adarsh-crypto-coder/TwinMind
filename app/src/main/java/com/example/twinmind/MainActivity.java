package com.example.twinmind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.services.calendar.CalendarScopes;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public class MainActivity extends AppCompatActivity {

    private LinearLayout btnGoogle;
    private static final int RC_SIGNIN = 9001;
    private static final String TAG = "GoogleSignIn";

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGoogle = findViewById(R.id.btn_google);
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .requestScopes(new Scope(CalendarScopes.CALENDAR_READONLY))// Request profile information including photo
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        btnGoogle.setOnClickListener(view -> handleSignIn());

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainScreen();
        }
    }

    private void handleSignIn() {
        Intent signIn = googleSignInClient.getSignInIntent();
        startActivityForResult(signIn, RC_SIGNIN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGNIN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

                // Store the Google account info for later use
                firebaseAuthWithGoogle(account.getIdToken(), account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInAccount googleAccount) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();

                        // Update user profile with Google account info if needed
                        updateUserProfile(user, googleAccount);

                        Toast.makeText(MainActivity.this, "Authentication successful!", Toast.LENGTH_SHORT).show();

                        // Navigate to next activity
                        navigateToMainScreen();
                    } else {
                        // Sign in failed
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, GoogleSignInAccount googleAccount) {
        if (user != null && googleAccount != null) {
            // Create profile update request
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(googleAccount.getDisplayName())
                    .setPhotoUri(googleAccount.getPhotoUrl())
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                        } else {
                            Log.w(TAG, "Failed to update user profile.", task.getException());
                        }
                    });
        }
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void signOut() {
        auth.signOut();

        // Google sign out
        googleSignInClient.signOut().addOnCompleteListener(this,
                task -> Toast.makeText(MainActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show());
    }


}