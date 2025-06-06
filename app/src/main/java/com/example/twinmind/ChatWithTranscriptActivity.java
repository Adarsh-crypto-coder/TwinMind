package com.example.twinmind;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatWithTranscriptActivity extends AppCompatActivity {

    private static final String TAG = "ChatTranscript";

    private RecyclerView recyclerView;
    private EditText inputField;
    private ImageView sendButton;
    private TextView titleText;
    private ProgressBar loadingProgress;
    private ImageView backButton;
    private View inputContainer;

    private ChatMessagesAdapter adapter;
    private List<ChatMessage> chatMessages;
    private OpenAIManager openAIManager;
    private TranscriptionDatabaseHelper dbHelper;
    private Handler mainHandler;
    private LinearLayoutManager layoutManager;

    private String sessionId;
    private String fullTranscript;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_transcript);

        sessionId = getIntent().getStringExtra("SESSION_ID");
        if (sessionId == null) {
            Toast.makeText(this, "No session provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupChat();
        setupKeyboardListener();
        loadTranscript();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
        inputField = findViewById(R.id.input_field);
        sendButton = findViewById(R.id.send_button);
        titleText = findViewById(R.id.title_text);
        loadingProgress = findViewById(R.id.loading_progress);
        backButton = findViewById(R.id.back_button);
        inputContainer = findViewById(R.id.input_container);

        backButton.setOnClickListener(v -> finish());

        titleText.setText("Chat with Transcript");

        sendButton.setOnClickListener(v -> sendMessage());
        sendButton.setEnabled(false);
        inputField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(!s.toString().trim().isEmpty() && !isProcessing);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupChat() {
        chatMessages = new ArrayList<>();
        adapter = new ChatMessagesAdapter(chatMessages);

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        openAIManager = new OpenAIManager(this);
        dbHelper = TranscriptionDatabaseHelper.getInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());

        addMessage("Hi! I'm ready to help you analyze and discuss your meeting transcript. What would you like to know?",
                ChatMessage.MessageType.ASSISTANT);
    }

    private void setupKeyboardListener() {
        View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private int previousHeight = 0;

            @Override
            public void onGlobalLayout() {
                int currentHeight = rootView.getHeight();

                if (previousHeight != 0) {
                    int heightDifference = previousHeight - currentHeight;
                    if (Math.abs(heightDifference) > 200) {
                        if (heightDifference > 0) {
                            onKeyboardShown();
                        } else {
                            onKeyboardHidden();
                        }
                    }
                }

                previousHeight = currentHeight;
            }
        });
    }

    private void onKeyboardShown() {
        scrollToBottom();
    }

    private void onKeyboardHidden() {
    }

    private void loadTranscript() {
        showLoading(true);
        new Thread(() -> {
            try {
                List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);

                StringBuilder transcriptBuilder = new StringBuilder();
                for (TranscriptionEntry entry : transcriptions) {
                    if (entry.transcriptionText != null && !entry.transcriptionText.trim().isEmpty()) {
                        transcriptBuilder.append(entry.transcriptionText.trim()).append(" ");
                    }
                }

                fullTranscript = transcriptBuilder.toString().trim();

                mainHandler.post(() -> {
                    showLoading(false);

                    if (fullTranscript.isEmpty()) {
                        addMessage("No transcript found for this session. Please make sure the recording has been transcribed.",
                                ChatMessage.MessageType.SYSTEM);
                    } else {
                        Log.d(TAG, "Loaded transcript: " + fullTranscript.length() + " characters");
                        inputField.setEnabled(true);
                        addMessage("Transcript loaded! You can now ask questions about your meeting.",
                                ChatMessage.MessageType.SYSTEM);
                        addSuggestionMessage();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading transcript", e);
                mainHandler.post(() -> {
                    showLoading(false);
                    addMessage("Error loading transcript: " + e.getMessage(), ChatMessage.MessageType.SYSTEM);
                });
            }
        }).start();
    }

    private void addSuggestionMessage() {
        String suggestions = "💡 Try asking:\n" +
                "• \"Summarize the key points\"\n" +
                "• \"What decisions were made?\"\n" +
                "• \"List any action items\"\n" +
                "• \"What topics were discussed?\"\n" +
                "• \"Who said what about [topic]?\"";

        addMessage(suggestions, ChatMessage.MessageType.SYSTEM);
    }

    private void sendMessage() {
        String userMessage = inputField.getText().toString().trim();
        if (userMessage.isEmpty() || isProcessing) return;
        hideKeyboard();
        addMessage(userMessage, ChatMessage.MessageType.USER);
        inputField.setText("");
        getAIResponse(userMessage);
    }

    private void getAIResponse(String userMessage) {
        isProcessing = true;
        sendButton.setEnabled(false);
        inputField.setEnabled(false);
        ChatMessage typingMessage = new ChatMessage("🤔 Thinking...", ChatMessage.MessageType.ASSISTANT);
        addMessage(typingMessage);
        String contextPrompt = buildContextPrompt(userMessage);

        openAIManager.generateText(contextPrompt, new OpenAIManager.OpenAICallback() {
            @Override
            public void onSuccess(String response) {
                mainHandler.post(() -> {
                    if (!chatMessages.isEmpty() && chatMessages.get(chatMessages.size() - 1).content.equals("🤔 Thinking...")) {
                        chatMessages.remove(chatMessages.size() - 1);
                        adapter.notifyItemRemoved(chatMessages.size());
                    }

                    addMessage(response, ChatMessage.MessageType.ASSISTANT);

                    isProcessing = false;
                    inputField.setEnabled(true);
                    sendButton.setEnabled(!inputField.getText().toString().trim().isEmpty());

                    Log.d(TAG, "AI response received successfully");
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    if (!chatMessages.isEmpty() && chatMessages.get(chatMessages.size() - 1).content.equals("🤔 Thinking...")) {
                        chatMessages.remove(chatMessages.size() - 1);
                        adapter.notifyItemRemoved(chatMessages.size());
                    }

                    addMessage("Sorry, I encountered an error: " + error, ChatMessage.MessageType.SYSTEM);

                    isProcessing = false;
                    inputField.setEnabled(true);
                    sendButton.setEnabled(!inputField.getText().toString().trim().isEmpty());

                    Log.e(TAG, "AI response error: " + error);
                });
            }
        });
    }

    private String buildContextPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI assistant helping analyze and discuss a meeting transcript. ");
        prompt.append("Answer questions about the content, provide summaries, extract key points, ");
        prompt.append("and help users understand the discussion. Be concise but thorough.\n\n");

        prompt.append("MEETING TRANSCRIPT:\n");
        prompt.append(fullTranscript);
        prompt.append("\n\n");

        if (chatMessages.size() > 1) {
            prompt.append("CONVERSATION HISTORY:\n");
            int startIndex = Math.max(0, chatMessages.size() - 6);
            for (int i = startIndex; i < chatMessages.size(); i++) {
                ChatMessage msg = chatMessages.get(i);
                if (msg.type != ChatMessage.MessageType.SYSTEM && !msg.content.equals("🤔 Thinking...")) {
                    String role = msg.type == ChatMessage.MessageType.USER ? "User" : "Assistant";
                    prompt.append(role).append(": ").append(msg.content).append("\n");
                }
            }
            prompt.append("\n");
        }

        prompt.append("USER QUESTION: ").append(userMessage).append("\n\n");
        prompt.append("Please provide a helpful response based on the transcript:");

        return prompt.toString();
    }

    private void addMessage(String content, ChatMessage.MessageType type) {
        ChatMessage message = new ChatMessage(content, type);
        addMessage(message);
    }

    private void addMessage(ChatMessage message) {
        chatMessages.add(message);
        adapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        if (!chatMessages.isEmpty()) {
            recyclerView.post(() -> {
                recyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            });
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        inputField.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (openAIManager != null) {
            openAIManager.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        hideKeyboard();
        super.onBackPressed();
    }
}