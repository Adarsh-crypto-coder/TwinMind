package com.example.twinmind;

import android.app.appsearch.SearchResult;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    private TranscriptionDatabaseHelper dbHelper;
    private Context context;
    private String testSessionId = "test_session_123";

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = TranscriptionDatabaseHelper.getInstance(context);
        dbHelper.clearAllData(); // Clean slate for each test
    }

    @After
    public void tearDown() {
        dbHelper.clearAllData();
    }

    @Test
    public void testCreateRecordingSession() {
        // Given
        String title = "Test Meeting";
        long startTime = System.currentTimeMillis();
        String location = "Test Location";

        // When
        dbHelper.createRecordingSession(testSessionId, title, startTime, location);
        RecordingSession session = dbHelper.getRecordingSession(testSessionId);

        // Then
        assertNotNull("Session should not be null", session);
        assertEquals("Session ID should match", testSessionId, session.sessionId);
        assertEquals("Title should match", title, session.title);
        assertEquals("Start time should match", startTime, session.startTime);
        assertEquals("Location should match", location, session.location);
    }

    @Test
    public void testInsertAndRetrieveTranscriptions() {
        // Given
        dbHelper.createRecordingSession(testSessionId, "Test", System.currentTimeMillis(), "Location");
        String transcriptionText1 = "First transcription";
        String transcriptionText2 = "Second transcription";
        long timestamp1 = System.currentTimeMillis();
        long timestamp2 = timestamp1 + 1000;

        // When
        dbHelper.insertTranscription(testSessionId, transcriptionText1, timestamp1, 0);
        dbHelper.insertTranscription(testSessionId, transcriptionText2, timestamp2, 1);

        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(testSessionId);

        // Then
        assertEquals("Should have 2 transcriptions", 2, transcriptions.size());
        assertEquals("First transcription text should match", transcriptionText1, transcriptions.get(0).transcriptionText);
        assertEquals("Second transcription text should match", transcriptionText2, transcriptions.get(1).transcriptionText);
    }

    @Test
    public void testDuplicateTranscriptionPrevention() {
        // Given
        dbHelper.createRecordingSession(testSessionId, "Test", System.currentTimeMillis(), "Location");
        String duplicateText = "Duplicate transcription";
        long timestamp = System.currentTimeMillis();

        // When - Insert same transcription twice
        dbHelper.insertTranscription(testSessionId, duplicateText, timestamp, 0);
        dbHelper.insertTranscription(testSessionId, duplicateText, timestamp, 0);

        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(testSessionId);

        // Then
        assertEquals("Should only have 1 transcription (duplicates prevented)", 1, transcriptions.size());
    }
}