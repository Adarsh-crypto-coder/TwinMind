package com.example.twinmind;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CalendarFragment extends Fragment implements GoogleCalendarService.CalendarCallback {

    private static final String TAG = "CalendarFragment";

    private GoogleCalendarService calendarService;
    private CalendarUIManager uiManager;
    private CalendarEventsAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        calendarService = new GoogleCalendarService(getContext());
        adapter = new CalendarEventsAdapter(new ArrayList<>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        uiManager = new CalendarUIManager(view, adapter);
        uiManager.setEventClickListener(this::onEventClick);
        loadCalendarEvents();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCalendar();
    }

    private void loadCalendarEvents() {
        Log.d(TAG, "Loading calendar events...");
        uiManager.showLoading();
        calendarService.loadUpcomingEvents(this);
    }

    private void refreshCalendar() {
        if (!uiManager.isLoading()) {
            Log.d(TAG, "Refreshing calendar events...");
            uiManager.showLoading("Refreshing events...");
            calendarService.refreshEvents(this);
        }
    }

    private void onEventClick(CalendarEvent event) {
        Log.d(TAG, "Event clicked: " + event.title);
        if (getActivity() != null) {
            android.widget.Toast.makeText(getActivity(),
                    "Event: " + event.title,
                    android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onEventsLoaded(List<CalendarEvent> events) {
        Log.d(TAG, "Events loaded successfully: " + events.size() + " events");

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                uiManager.showEvents(events);
            });
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Calendar error: " + error);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                uiManager.showError(error);
            });
        }
    }

    @Override
    public void onPermissionRequired(String message) {
        Log.w(TAG, "Permission required: " + message);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                uiManager.showNoPermission(message);
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (calendarService != null) {
            calendarService.shutdown();
        }
    }
    public void forceRefresh() {
        loadCalendarEvents();
    }

    public boolean hasCalendarPermission() {
        return calendarService != null && calendarService.hasCalendarPermission();
    }

    public boolean isSignedIn() {
        return calendarService != null && calendarService.isSignedIn();
    }

    public int getEventCount() {
        return uiManager != null ? uiManager.getEventCount() : 0;
    }
}