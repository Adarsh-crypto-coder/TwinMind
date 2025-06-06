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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoriesFragment extends Fragment {

    private static final String TAG = "MemoriesFragment";

    private RecyclerView recyclerView;
    private LinearLayout tvNoMemories;
    private MemoriesAdapter adapter;
    private TranscriptionDatabaseHelper dbHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = TranscriptionDatabaseHelper.getInstance(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_memories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        loadMemories();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMemories();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_memories);
        tvNoMemories = view.findViewById(R.id.tv_no_memories);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MemoriesAdapter(this::onMemoryClick);
        recyclerView.setAdapter(adapter);
    }

    private void loadMemories() {
        List<SessionSummary> sessions = dbHelper.getAllSessionSummaries();

        if (sessions == null || sessions.isEmpty()) {
            showNoMemories();
            return;
        }

        Log.d(TAG, "Loaded " + sessions.size() + " memory sessions");
        adapter.setSessions(sessions);
        showMemories();
    }

    private void showMemories() {
        recyclerView.setVisibility(View.VISIBLE);
        tvNoMemories.setVisibility(View.GONE);
    }

    private void showNoMemories() {
        recyclerView.setVisibility(View.GONE);
        tvNoMemories.setVisibility(View.VISIBLE);
    }

    private void onMemoryClick(SessionSummary session) {
        Log.d(TAG, "Memory clicked: " + session.sessionId);

        if (getActivity() instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) getActivity();
            homeActivity.showNotesForSession(session.sessionId);
        }
    }

    private static class MemoriesAdapter extends RecyclerView.Adapter<MemoriesAdapter.MemoryViewHolder> {

        private List<SessionSummary> sessions;
        private OnMemoryClickListener clickListener;
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
        private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        public interface OnMemoryClickListener {
            void onMemoryClick(SessionSummary session);
        }

        public MemoriesAdapter(OnMemoryClickListener clickListener) {
            this.clickListener = clickListener;
        }

        public void setSessions(List<SessionSummary> sessions) {
            this.sessions = sessions;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MemoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_memory, parent, false);
            return new MemoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MemoryViewHolder holder, int position) {
            SessionSummary session = sessions.get(position);
            holder.bind(session, dateFormat, timeFormat, clickListener);
        }

        @Override
        public int getItemCount() {
            return sessions != null ? sessions.size() : 0;
        }

        static class MemoryViewHolder extends RecyclerView.ViewHolder {
            private TextView tvDate;
            private TextView tvTime;
            private TextView tvDuration;

            public MemoryViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvDuration = itemView.findViewById(R.id.tv_duration);
            }

            public void bind(SessionSummary session, SimpleDateFormat dateFormat,
                             SimpleDateFormat timeFormat, OnMemoryClickListener clickListener) {

                Date sessionDate = new Date(session.startTime);
                tvDate.setText(formatDateForGrouping(sessionDate, dateFormat));
                tvTime.setText(timeFormat.format(sessionDate));
                tvDuration.setText(formatDuration(session.duration));
                itemView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onMemoryClick(session);
                    }
                });
            }

            private String formatDateForGrouping(Date date, SimpleDateFormat dateFormat) {
                Calendar cal = Calendar.getInstance();
                Calendar today = Calendar.getInstance();
                cal.setTime(date);
                if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                    return "Today";
                }
                today.add(Calendar.DAY_OF_YEAR, -1);
                if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                    return "Yesterday";
                }
                return dateFormat.format(date);
            }

            private String generateTitle(String firstTranscription) {
                if (firstTranscription == null || firstTranscription.trim().isEmpty()) {
                    return "TwinMind Session";
                }

                String title = firstTranscription.trim();
                if (title.length() > 50) {
                    title = title.substring(0, 47) + "...";
                }

                if (title.length() > 0) {
                    title = title.substring(0, 1).toUpperCase() + title.substring(1);
                }

                return title;
            }

            private String formatDuration(long durationMs) {
                long seconds = durationMs / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;

                if (hours > 0) {
                    return String.format(Locale.getDefault(), "%dh %dm", hours, minutes % 60);
                } else if (minutes > 0) {
                    return String.format(Locale.getDefault(), "%dm", minutes);
                } else {
                    return String.format(Locale.getDefault(), "%ds", seconds);
                }
            }
        }
    }
}