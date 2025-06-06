package com.example.twinmind;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TranscriptFragment extends Fragment {

    private static final String TAG = "TranscriptFragment";
    private static final String ARG_SESSION_ID = "session_id";

    private String sessionId;
    private RecyclerView recyclerView;
    private LinearLayout tvNoTranscript;
    private TranscriptAdapter adapter;

    private TranscriptionDatabaseHelper dbHelper;

    public static TranscriptFragment newInstance(String sessionId) {
        TranscriptFragment fragment = new TranscriptFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transcript, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadTranscript();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        tvNoTranscript = view.findViewById(R.id.tv_no_transcript);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TranscriptAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadTranscript() {
        List<TranscriptionEntry> transcriptions = dbHelper.getTranscriptionsForSession(sessionId);

        if (transcriptions == null || transcriptions.isEmpty()) {
            showNoTranscript();
            return;
        }

        Log.d(TAG, "Loaded " + transcriptions.size() + " transcription entries");
        adapter.setTranscriptions(transcriptions);
        showTranscript();
    }

    private void showTranscript() {
        recyclerView.setVisibility(View.VISIBLE);
        tvNoTranscript.setVisibility(View.GONE);
    }

    private void showNoTranscript() {
        recyclerView.setVisibility(View.GONE);
        tvNoTranscript.setVisibility(View.VISIBLE);
    }

    private static class TranscriptAdapter extends RecyclerView.Adapter<TranscriptAdapter.TranscriptViewHolder> {

        private List<TranscriptionEntry> transcriptions;
        private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm:ss a", Locale.getDefault());

        public void setTranscriptions(List<TranscriptionEntry> transcriptions) {
            this.transcriptions = transcriptions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public TranscriptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transcript, parent, false);
            return new TranscriptViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TranscriptViewHolder holder, int position) {
            TranscriptionEntry entry = transcriptions.get(position);
            holder.bind(entry, timeFormat);
        }

        @Override
        public int getItemCount() {
            return transcriptions != null ? transcriptions.size() : 0;
        }

        static class TranscriptViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTimestamp;
            private TextView tvTranscription;

            public TranscriptViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
                tvTranscription = itemView.findViewById(R.id.tv_transcription);
            }

            public void bind(TranscriptionEntry entry, SimpleDateFormat timeFormat) {
                Date timestamp = new Date(entry.timestamp);
                tvTimestamp.setText(timeFormat.format(timestamp));
                tvTranscription.setText(entry.transcriptionText);
            }
        }
    }
}