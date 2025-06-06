package com.example.twinmind;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalendarUIManager {

    private RecyclerView recyclerView;
    private LinearLayout noEventsLayout;
    private LinearLayout noPermissionLayout;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvPermissionMessage;

    private CalendarEventsAdapter adapter;

    public CalendarUIManager(View rootView, CalendarEventsAdapter adapter) {
        this.adapter = adapter;
        initViews(rootView);
    }

    private void initViews(View rootView) {
        recyclerView = rootView.findViewById(R.id.recycler_events);
        noEventsLayout = rootView.findViewById(R.id.layout_no_events);
        noPermissionLayout = rootView.findViewById(R.id.layout_no_permission);
        progressBar = rootView.findViewById(R.id.progress_bar);
        tvStatus = rootView.findViewById(R.id.tv_status);
        tvPermissionMessage = noPermissionLayout.findViewById(R.id.tv_permission_message);

        recyclerView.setAdapter(adapter);
    }

    public void showLoading() {
        showLoading("Loading calendar events...");
    }

    public void showLoading(String message) {
        hideAllViews();
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
    }

    public void showEvents(List<CalendarEvent> events) {
        hideAllViews();

        if (events == null || events.isEmpty()) {
            showNoEvents();
            return;
        }

        adapter.setEvents(events);
        recyclerView.setVisibility(View.VISIBLE);
    }

    public void showNoEvents() {
        hideAllViews();
        noEventsLayout.setVisibility(View.VISIBLE);
    }

    public void showNoPermission(String message) {
        hideAllViews();
        noPermissionLayout.setVisibility(View.VISIBLE);

        if (tvPermissionMessage != null) {
            tvPermissionMessage.setText(message);
        }
    }

    public void showError(String errorMessage) {
        hideAllViews();
        if (recyclerView.getContext() != null) {
            Toast.makeText(recyclerView.getContext(), errorMessage, Toast.LENGTH_LONG).show();
        }
        showNoEvents();
    }

    private void hideAllViews() {
        recyclerView.setVisibility(View.GONE);
        noEventsLayout.setVisibility(View.GONE);
        noPermissionLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvStatus.setVisibility(View.GONE);
    }

    public void setEventClickListener(CalendarEventsAdapter.OnEventClickListener listener) {
        adapter.setOnEventClickListener(listener);
    }
    public boolean isShowingEvents() {
        return recyclerView.getVisibility() == View.VISIBLE;
    }

    public boolean isLoading() {
        return progressBar.getVisibility() == View.VISIBLE;
    }

    public int getEventCount() {
        return adapter.getItemCount();
    }
}