package com.example.twinmind;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ChatWithTranscriptTest {

    private Context context;
    private TranscriptionDatabaseHelper dbHelper;
    private String testSessionId = "chat_test_session";

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = TranscriptionDatabaseHelper.getInstance(context);
        dbHelper.clearAllData();

        // Setup test data
        setupTestTranscript();
    }

    private void setupTestTranscript() {
        dbHelper.createRecordingSession(testSessionId, "Test Meeting", System.currentTimeMillis(), "Test Location");
        dbHelper.insertTranscription(testSessionId, "Welcome everyone to the meeting", System.currentTimeMillis(), 0);
        dbHelper.insertTranscription(testSessionId, "Let's discuss the project timeline", System.currentTimeMillis() + 1000, 1);
        dbHelper.insertTranscription(testSessionId, "Sarah will handle the design phase", System.currentTimeMillis() + 2000, 2);
        dbHelper.insertTranscription(testSessionId, "Mike is responsible for backend development", System.currentTimeMillis() + 3000, 3);
        dbHelper.insertTranscription(testSessionId, "We need to deliver by end of Q1", System.currentTimeMillis() + 4000, 4);
    }

    @Test
    public void testChatMessageCreation() {
        // Given
        String messageContent = "What were the main topics discussed?";
        ChatMessage.MessageType messageType = ChatMessage.MessageType.USER;

        // When
        ChatMessage message = new ChatMessage(messageContent, messageType);

        // Then
        assertEquals("Content should match", messageContent, message.content);
        assertEquals("Type should match", messageType, message.type);
        assertNotNull("ID should be generated", message.id);
        assertTrue("Timestamp should be valid", message.timestamp > 0);
    }

    @Test
    public void testTranscriptLoading() {
        // When
        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(testSessionId);

        // Then
        assertNotNull("Transcriptions should not be null", transcriptions);
        assertEquals("Should have 5 transcriptions", 5, transcriptions.size());

        // Verify content
        assertTrue("Should contain meeting content",
                transcriptions.get(0).transcriptionText.contains("Welcome"));
        assertTrue("Should contain timeline discussion",
                transcriptions.get(1).transcriptionText.contains("timeline"));
    }

    @Test
    public void testChatContextBuilding() {
        // Given
        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(testSessionId);
        StringBuilder fullTranscript = new StringBuilder();

        for (TranscriptionEntry entry : transcriptions) {
            fullTranscript.append(entry.transcriptionText).append(" ");
        }

        String transcript = fullTranscript.toString().trim();

        // When
        String contextPrompt = buildChatContext("What are the action items?", transcript);

        // Then
        assertNotNull("Context should not be null", contextPrompt);
        assertTrue("Context should contain transcript", contextPrompt.contains("Welcome everyone"));
        assertTrue("Context should contain user question", contextPrompt.contains("action items"));
        assertTrue("Context should contain instructions", contextPrompt.toLowerCase().contains("assistant"));
    }

    private String buildChatContext(String userQuestion, String transcript) {
        return "You are an AI assistant analyzing a meeting transcript. " +
                "Transcript: " + transcript + " " +
                "User question: " + userQuestion;
    }

}
