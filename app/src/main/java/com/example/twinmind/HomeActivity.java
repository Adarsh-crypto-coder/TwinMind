
package com.example.twinmind;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ViewPager2 viewPager;
    private TextView tabMemories, tabCalendar, tabQuestions;
    private ImageView profileImage;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private int currentTab = 0;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private CardView btnCapture;

    // Use consistent permission request code
    private static final int PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initFirebase();
        loadUserProfile();
        setupViewPager();
        setupTabClickListeners();
        setupNavigationDrawer();
    }

    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        tabMemories = findViewById(R.id.tab_memories);
        tabCalendar = findViewById(R.id.tab_calendar);
        tabQuestions = findViewById(R.id.tab_questions);
        profileImage = findViewById(R.id.profile_image);
        btnCapture = findViewById(R.id.btn_capture);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
    }

    private void initFirebase() {
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this);

        // Setup profile image click to open drawer
        profileImage.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Update navigation header with user info
        updateNavigationHeader();
    }

    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        ImageView navProfileImage = headerView.findViewById(R.id.nav_profile_image);
        TextView navUserName = headerView.findViewById(R.id.nav_user_name);
        TextView navUserEmail = headerView.findViewById(R.id.nav_user_email);

        if (currentUser != null) {
            // Set user name
            String displayName = currentUser.getDisplayName();
            navUserName.setText(displayName != null ? displayName : "User");

            // Set user email
            String email = currentUser.getEmail();
            navUserEmail.setText(email != null ? email : "");

            // Load profile image
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(navProfileImage);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

          if (id == R.id.nav_sign_out) {
            // Sign out user
            signOutUser();
            return true;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void signOutUser() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    performSignOut();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSignOut() {
        try {
            auth.signOut();
            clearLocalData();

            // Navigate to login activity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("HomeActivity", "Error signing out", e);
            Toast.makeText(this, "Error signing out. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearLocalData() {
        // Clear any cached data, preferences, etc.
        // For example:
        // SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        // prefs.edit().clear().apply();

        // Clear database if needed (be careful with this)
        // TranscriptionDatabaseHelper.getInstance(this).clearAllData();
    }

    private void loadUserProfile() {
        if (currentUser != null) {
            // Get profile photo URL
            if (currentUser.getPhotoUrl() != null) {
                String photoUrl = currentUser.getPhotoUrl().toString();

                // Load profile image using Glide
                Glide.with(this)
                        .load(photoUrl)
                        .apply(RequestOptions.circleCropTransform()) // Makes image circular
                        .placeholder(R.drawable.ic_profile) // Fallback image
                        .error(R.drawable.ic_profile) // Error image
                        .into(profileImage);
            }

            // Optional: You can also get other user info
            String displayName = currentUser.getDisplayName();
            String email = currentUser.getEmail();

            // You can use these for other parts of your UI if needed
        }
    }

    private void setupViewPager() {
        TabsAdapter adapter = new TabsAdapter(this);
        viewPager.setAdapter(adapter);

        // Handle page changes when swiping
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
            }
        });
    }

    private void setupTabClickListeners() {
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleCaptureClick();
            }
        });

        tabMemories.setOnClickListener(v -> {
            viewPager.setCurrentItem(0, true);
        });

        tabCalendar.setOnClickListener(v -> {
            viewPager.setCurrentItem(1, true);
        });

        tabQuestions.setOnClickListener(v -> {
            viewPager.setCurrentItem(2, true);
        });
    }

    private void handleCaptureClick() {
        Log.d("HomeActivity", "Capture button clicked");

        if (checkAllPermissions()) {
            // All permissions granted, start recording activity immediately
            Log.d("HomeActivity", "All permissions granted, starting recording");
            startRecordingActivity();
        } else {
            // Request all required permissions
            Log.d("HomeActivity", "Permissions not granted, requesting permissions");
            requestAllPermissions();
        }
    }

    private void updateTabSelection(int position) {
        currentTab = position;

        // Reset all tabs to inactive state
        tabMemories.setTextColor(getColor(R.color.inactive_tab));
        tabMemories.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabCalendar.setTextColor(getColor(R.color.inactive_tab));
        tabCalendar.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabQuestions.setTextColor(getColor(R.color.inactive_tab));
        tabQuestions.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Set active tab
        switch (position) {
            case 0:
                tabMemories.setTextColor(getColor(R.color.active_tab));
                tabMemories.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 1:
                tabCalendar.setTextColor(getColor(R.color.active_tab));
                tabCalendar.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 2:
                tabQuestions.setTextColor(getColor(R.color.active_tab));
                tabQuestions.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
        }
    }

    // ViewPager2 Adapter
    private static class TabsAdapter extends FragmentStateAdapter {
        public TabsAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new MemoriesFragment();
                case 1:
                    return new CalendarFragment();
                case 2:
                    return new QuestionsFragment();
                default:
                    return new MemoriesFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Number of tabs
        }
    }

    // Check all required permissions
    private boolean checkAllPermissions() {
        boolean micPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        // For Android 11+ (API 30+), WRITE_EXTERNAL_STORAGE is not needed for app-specific directories
        // For older versions, we still check it
        boolean storagePermission = true;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            storagePermission = ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        Log.d("HomeActivity", "Microphone permission: " + micPermission);
        Log.d("HomeActivity", "Storage permission: " + storagePermission);
        Log.d("HomeActivity", "Android version: " + android.os.Build.VERSION_CODES.R);

        return micPermission && storagePermission;
    }

    // Request all required permissions at once
    private void requestAllPermissions() {
        // Only request microphone for Android 11+, include storage for older versions
        String[] permissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            permissions = new String[]{
                    android.Manifest.permission.RECORD_AUDIO
            };
            Log.d("HomeActivity", "Requesting only microphone permission for Android 11+");
        } else {
            permissions = new String[]{
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            Log.d("HomeActivity", "Requesting microphone and storage permissions for older Android");
        }

        Log.d("HomeActivity", "Requesting permissions: " + java.util.Arrays.toString(permissions));
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("HomeActivity", "Permission result received. Request code: " + requestCode);
        Log.d("HomeActivity", "Permissions: " + java.util.Arrays.toString(permissions));
        Log.d("HomeActivity", "Grant results: " + java.util.Arrays.toString(grantResults));

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            // Check if all permissions were granted
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;

                    // Show specific error message for denied permission
                    if (permissions[i].equals(android.Manifest.permission.RECORD_AUDIO)) {
                        Toast.makeText(this, "Microphone permission is required to record audio",
                                Toast.LENGTH_LONG).show();
                        Log.d("HomeActivity", "Microphone permission denied");
                    } else if (permissions[i].equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, "Storage permission is required to save recordings",
                                Toast.LENGTH_LONG).show();
                        Log.d("HomeActivity", "Storage permission denied");
                    }
                    break;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
                Log.d("HomeActivity", "All permissions granted, starting recording activity");
                startRecordingActivity();
            } else {
                Toast.makeText(this, "All permissions are required for recording",
                        Toast.LENGTH_LONG).show();
                Log.d("HomeActivity", "Some permissions denied");
            }
        }
    }

    private void startRecordingActivity() {
        Intent intent = new Intent(this, RecordingActivity.class);
        // Pass a flag to indicate permissions are already checked
        intent.putExtra("PERMISSIONS_GRANTED", true);
        startActivity(intent);
    }

    public void showNotesForSession(String sessionId) {
        Log.d("HomeActivity", "Navigating to notes for session: " + sessionId);

        // Start a new activity for the notes view since you're using ViewPager2 for tabs
        Intent intent = new Intent(this, SessionNotesActivity.class);
        intent.putExtra("SESSION_ID", sessionId);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}