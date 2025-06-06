package com.example.twinmind;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class NotesFragment extends Fragment implements OpenAIManager.OpenAICallback {

    private static final String TAG = "NotesFragment";
    private static final String ARG_SESSION_ID = "session_id";

    private String sessionId;
    private TextView tvSummary;
    private TextView tvActionItems;
    private TextView tvNotes;
    private ProgressBar progressBar;
    private View contentContainer;
    private View loadingContainer;
    private Button btnChatTranscript; // Added chat button

    private TranscriptionDatabaseHelper dbHelper;
    private OpenAIManager openAIManager;

    public static NotesFragment newInstance(String sessionId) {
        NotesFragment fragment = new NotesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sessionId = getArguments().getString(ARG_SESSION_ID);
        }

        dbHelper = TranscriptionDatabaseHelper.getInstance(getContext());
        openAIManager = new OpenAIManager(getContext());

        Log.d(TAG, "NotesFragment created with session ID: " + sessionId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupChatButton();
        generateNotes();
    }

    private void initViews(View view) {
        tvSummary = view.findViewById(R.id.tv_summary);
        tvActionItems = view.findViewById(R.id.tv_action_items);
        tvNotes = view.findViewById(R.id.tv_notes);
        progressBar = view.findViewById(R.id.progress_bar);
        contentContainer = view.findViewById(R.id.content_container);
        loadingContainer = view.findViewById(R.id.loading_container);
        btnChatTranscript = view.findViewById(R.id.btn_chat_transcript);
    }

    private void setupChatButton() {
        if (btnChatTranscript != null) {
            btnChatTranscript.setOnClickListener(v -> {
                Log.d(TAG, "Chat with transcript button clicked for session: " + sessionId);
                openChatWithTranscript();
            });
            Log.d(TAG, "Chat button setup successfully");
        } else {
            Log.w(TAG, "Chat button not found in layout");
        }
    }

    private void openChatWithTranscript() {
        if (sessionId == null || sessionId.isEmpty()) {
            Toast.makeText(getContext(), "Session not available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);
        if (transcriptions == null || transcriptions.isEmpty()) {
            Toast.makeText(getContext(), "No transcript available for this session", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), ChatWithTranscriptActivity.class);
        intent.putExtra("SESSION_ID", sessionId);
        startActivity(intent);

        Log.d(TAG, "Opening chat for session: " + sessionId);
    }

    private void generateNotes() {
        Log.d(TAG, "=== STARTING NOTES GENERATION ===");
        Log.d(TAG, "Session ID: " + sessionId);

        showLoading(true);
        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);

        Log.d(TAG, "Found transcriptions: " + (transcriptions != null ? transcriptions.size() : 0));

        if (transcriptions == null || transcriptions.isEmpty()) {
            Log.w(TAG, "No transcriptions found - will retry in 3 seconds");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                retryGenerateNotes();
            }, 3000);
            return;
        }
        processTranscriptions(transcriptions);
    }

    private void retryGenerateNotes() {
        Log.d(TAG, "=== RETRYING NOTES GENERATION ===");

        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);
        Log.d(TAG, "Retry - Found transcriptions: " + (transcriptions != null ? transcriptions.size() : 0));

        if (transcriptions == null || transcriptions.isEmpty()) {
            Log.w(TAG, "Still no transcriptions found - showing error");
            showError("No transcription data available");
            return;
        }

        processTranscriptions(transcriptions);
    }

    private void processTranscriptions(List<TranscriptionEntry> transcriptions) {
        StringBuilder fullTranscript = new StringBuilder();
        for (TranscriptionEntry entry : transcriptions) {
            fullTranscript.append(entry.transcriptionText).append(" ");
            Log.d(TAG, "Adding transcription: " + entry.transcriptionText);
        }

        String transcriptText = fullTranscript.toString().trim();

        if (transcriptText.isEmpty()) {
            Log.w(TAG, "Transcript text is empty - showing error");
            showError("No transcription content available");
            return;
        }

        Log.d(TAG, "Full transcript: " + transcriptText);
        Log.d(TAG, "Transcript length: " + transcriptText.length());

        generateSummaryAndNotes(transcriptText);
    }

    private void generateSummaryAndNotes(String transcriptText) {
        String prompt = createNotesPrompt(transcriptText);

        Log.d(TAG, "Calling OpenAI with prompt length: " + prompt.length());

        openAIManager.generateText(prompt, new OpenAIManager.OpenAICallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "OpenAI Success: " + response);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        parseAndDisplayNotes(response);
                        showLoading(false);
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "OpenAI Error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showError("Failed to generate notes: " + error);
                    });
                }
            }
        });
    }

    private String createNotesPrompt(String transcriptText) {
        return "Please analyze the following meeting/conversation transcript and provide a structured summary in the following format:\n\n" +
                "## Summary\n" +
                "[Provide a concise 2-3 sentence overview of the main topics discussed]\n\n" +
                "## Meeting Process Overview\n" +
                "[Outline the flow and structure of the meeting/conversation]\n\n" +
                "## Action Items\n" +
                "[List specific action items, tasks, or next steps mentioned]\n\n" +
                "## Your Notes\n" +
                "[Provide detailed notes organized by topic or speaker, including key decisions, important points, and any other relevant information]\n\n" +
                "Transcript:\n" + transcriptText + "\n\n" +
                "Please ensure the response is well-formatted and easy to read. If the transcript is unclear or incomplete, work with what's available and note any limitations.";
    }

    private void parseAndDisplayNotes(String response) {
        Log.d(TAG, "Parsing AI response: " + response.substring(0, Math.min(100, response.length())) + "...");

        try {
            String[] sections = response.split("##");

            StringBuilder summaryText = new StringBuilder();
            StringBuilder actionItemsText = new StringBuilder();
            StringBuilder notesText = new StringBuilder();

            for (String section : sections) {
                String trimmed = section.trim();
                if (trimmed.toLowerCase().startsWith("summary")) {
                    summaryText.append(trimmed.substring(trimmed.indexOf('\n') + 1).trim());
                } else if (trimmed.toLowerCase().contains("meeting process overview")) {
                    summaryText.append("\n\n").append(trimmed.substring(trimmed.indexOf('\n') + 1).trim());
                } else if (trimmed.toLowerCase().contains("action items")) {
                    actionItemsText.append(trimmed.substring(trimmed.indexOf('\n') + 1).trim());
                } else if (trimmed.toLowerCase().contains("your notes") || trimmed.toLowerCase().contains("notes")) {
                    notesText.append(trimmed.substring(trimmed.indexOf('\n') + 1).trim());
                }
            }

            if (summaryText.length() > 0) {
                tvSummary.setText(summaryText.toString());
            } else {
                tvSummary.setText("Summary not available");
            }

            if (actionItemsText.length() > 0) {
                tvActionItems.setText(actionItemsText.toString());
            } else {
                tvActionItems.setText("No action items identified");
            }

            if (notesText.length() > 0) {
                tvNotes.setText(notesText.toString());
            } else {
                tvNotes.setText("Detailed notes not available");
            }

            Log.d(TAG, "Notes successfully parsed and displayed");

        } catch (Exception e) {
            Log.e(TAG, "Error parsing AI response", e);
            // Fallback: display the raw response
            tvSummary.setText("Summary");
            tvActionItems.setText("Action items not parsed");
            tvNotes.setText(response);
        }
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            loadingContainer.setVisibility(View.VISIBLE);
            contentContainer.setVisibility(View.GONE);
            Log.d(TAG, "Showing loading state");
        } else {
            loadingContainer.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
            Log.d(TAG, "Hiding loading state");
        }
    }

    private void showError(String message) {
        showLoading(false);
        tvSummary.setText("Error");
        tvActionItems.setText("Unable to generate action items");
        tvNotes.setText(message);

        Log.e(TAG, "Showing error: " + message);

        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSuccess(String response) {
        // This method can be used for additional OpenAI calls if needed
    }

    @Override
    public void onError(String error) {
        // This method can be used for additional OpenAI calls if needed
    }
}