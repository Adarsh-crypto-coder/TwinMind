package com.example.twinmind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_session_notes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main3), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

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
        NotesFragment notesFragment = NotesFragment.newInstance(sessionId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, notesFragment)
                .commit();
    }

    private void setActivityTitle() {
        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);

        if (transcriptions != null && !transcriptions.isEmpty()) {
            String firstText = transcriptions.get(0).transcriptionText;
            String title = generateTitleFromText(firstText);
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
        String title = text.trim();
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }
        if (title.length() > 0) {
            title = title.substring(0, 1).toUpperCase() + title.substring(1);
        }

        return title;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}