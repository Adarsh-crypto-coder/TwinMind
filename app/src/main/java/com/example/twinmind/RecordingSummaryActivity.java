package com.example.twinmind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordingSummaryActivity extends AppCompatActivity {

    private static final String TAG = "RecordingSummaryActivity";

    private ViewPager2 viewPager;
    private TextView tabQuestions, tabNotes, tabTranscript;
    private TextView tvDate, tvTime, tvLocation, tvDuration, tvTranscriptionCount;
    private ImageView btnBack, btnShare;

    private String sessionId;
    private String recordingDuration;
    private int transcriptionCount;
    private TranscriptionDatabaseHelper dbHelper;
    private OpenAIManager openAIManager;

    private int currentTab = 1; // Start with Notes tab (middle)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_summary);

        initViews();
        initData();
        setupViewPager();
        setupClickListeners();
        loadRecordingDetails();
    }

    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        tabQuestions = findViewById(R.id.tab_questions);
        tabNotes = findViewById(R.id.tab_notes);
        tabTranscript = findViewById(R.id.tab_transcript);
        tvDate = findViewById(R.id.tv_date);
        tvTime = findViewById(R.id.tv_time);
        tvLocation = findViewById(R.id.tv_location);
        tvDuration = findViewById(R.id.tv_duration);
        tvTranscriptionCount = findViewById(R.id.tv_transcription_count);
        btnBack = findViewById(R.id.btn_back);
        btnShare = findViewById(R.id.btn_share);
    }

    private void initData() {
        // Get data from intent
        sessionId = getIntent().getStringExtra("SESSION_ID");
        recordingDuration = getIntent().getStringExtra("RECORDING_DURATION");
        transcriptionCount = getIntent().getIntExtra("TRANSCRIPTION_COUNT", 0);

        // Initialize managers
        dbHelper = TranscriptionDatabaseHelper.getInstance(this);
        openAIManager = new OpenAIManager(this);

        Log.d(TAG, "Session ID: " + sessionId);
        Log.d(TAG, "Duration: " + recordingDuration);
        Log.d(TAG, "Transcription count: " + transcriptionCount);
    }

    private void loadRecordingDetails() {
        // Load session details from database
        RecordingSession session = dbHelper.getRecordingSession(sessionId);
        if (session != null) {
            // Format date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

            Date sessionDate = new Date(session.startTime);
            tvDate.setText(dateFormat.format(sessionDate));
            tvTime.setText(timeFormat.format(sessionDate));
            tvLocation.setText(session.location != null ? session.location : "Unknown Location");
        }

        // Set duration and transcription count
        if (recordingDuration != null) {
            tvDuration.setText(recordingDuration);
        }
        tvTranscriptionCount.setText(String.valueOf(transcriptionCount));
    }

    private void setupViewPager() {
        SummaryTabsAdapter adapter = new SummaryTabsAdapter(this);
        viewPager.setAdapter(adapter);

        // Set initial tab to Notes (middle tab)
        viewPager.setCurrentItem(1, false);
        updateTabSelection(1);

        // Handle page changes when swiping
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateTabSelection(position);
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            // Go back to home activity
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnShare.setOnClickListener(v -> {
            shareRecording();
        });

        tabQuestions.setOnClickListener(v -> {
            viewPager.setCurrentItem(0, true);
        });

        tabNotes.setOnClickListener(v -> {
            viewPager.setCurrentItem(1, true);
        });

        tabTranscript.setOnClickListener(v -> {
            viewPager.setCurrentItem(2, true);
        });
    }

    private void updateTabSelection(int position) {
        currentTab = position;

        // Reset all tabs to inactive state
        tabQuestions.setTextColor(getColor(R.color.inactive_tab));
        tabQuestions.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabNotes.setTextColor(getColor(R.color.inactive_tab));
        tabNotes.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabTranscript.setTextColor(getColor(R.color.inactive_tab));
        tabTranscript.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Set active tab
        switch (position) {
            case 0:
                tabQuestions.setTextColor(getColor(R.color.active_tab));
                tabQuestions.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 1:
                tabNotes.setTextColor(getColor(R.color.active_tab));
                tabNotes.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
            case 2:
                tabTranscript.setTextColor(getColor(R.color.active_tab));
                tabTranscript.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
        }
    }

    private void shareRecording() {
        try {
            // Get transcript text
            List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);
            StringBuilder fullTranscript = new StringBuilder();

            for (TranscriptionEntry entry : transcriptions) {
                fullTranscript.append(entry.transcriptionText).append("\n\n");
            }

            String shareText = "Recording Summary\n" +
                    "Date: " + tvDate.getText() + "\n" +
                    "Time: " + tvTime.getText() + "\n" +
                    "Duration: " + tvDuration.getText() + "\n\n" +
                    "Transcript:\n" + fullTranscript.toString();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Recording Summary");

            startActivity(Intent.createChooser(shareIntent, "Share Recording"));

        } catch (Exception e) {
            Log.e(TAG, "Error sharing recording", e);
            Toast.makeText(this, "Error sharing recording", Toast.LENGTH_SHORT).show();
        }
    }

    // ViewPager2 Adapter for Summary Tabs
    private class SummaryTabsAdapter extends FragmentStateAdapter {
        public SummaryTabsAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return QuestionsFragment.newInstance(sessionId);
                case 1:
                    return NotesFragment.newInstance(sessionId);
                case 2:
                    return TranscriptFragment.newInstance(sessionId);
                default:
                    return NotesFragment.newInstance(sessionId);
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Questions, Notes, Transcript
        }
    }

    @Override
    public void onBackPressed() {
        // Go back to home activity instead of previous activity
        super.onBackPressed();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}