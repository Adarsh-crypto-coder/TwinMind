package com.example.twinmind;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionNotesActivity extends AppCompatActivity {

    private static final String TAG = "SessionNotesActivity";
    private String sessionId;
    private TranscriptionDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_notes);

        // Get session ID from intent
        sessionId = getIntent().getStringExtra("SESSION_ID");
        if (sessionId == null) {
            Log.e(TAG, "No session ID provided");
            finish();
            return;
        }

        dbHelper = TranscriptionDatabaseHelper.getInstance(this);

        setupToolbar();
        loadNotesFragment();
        setActivityTitle();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Session Notes");
        }
    }

    private void loadNotesFragment() {
        // Use your existing NotesFragment with the session ID
        NotesFragment notesFragment = NotesFragment.newInstance(sessionId);

        // Replace fragment container with your NotesFragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, notesFragment)
                .commit();
    }

    private void setActivityTitle() {
        // Get session info and set a meaningful title
        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);

        if (transcriptions != null && !transcriptions.isEmpty()) {
            // Get the first transcription for title
            String firstText = transcriptions.get(0).transcriptionText;
            String title = generateTitleFromText(firstText);

            // Get session date
            Date sessionDate = new Date(transcriptions.get(0).timestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
            String dateStr = dateFormat.format(sessionDate);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setSubtitle(dateStr);
            }
        }
    }

    private String generateTitleFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Recording Session";
        }

        // Take first meaningful words (up to 50 characters)
        String title = text.trim();
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }

        // Capitalize first letter
        if (title.length() > 0) {
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
        }

        return title;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle back button click
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Return to HomeActivity
        finish();
    }
}