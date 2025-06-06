package com.example.twinmind;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoogleCalendarService {

    private static final String TAG = "GoogleCalendarService";

    private Context context;
    private Calendar calendarService;
    private ExecutorService executorService;

    public interface CalendarCallback {
        void onEventsLoaded(List<CalendarEvent> events);
        void onError(String error);
        void onPermissionRequired(String message);
    }

    public GoogleCalendarService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void loadUpcomingEvents(CalendarCallback callback) {
        loadUpcomingEvents(30, callback); // Default 30 days
    }

    public void loadUpcomingEvents(int daysAhead, CalendarCallback callback) {

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);

        if (account == null) {
            callback.onPermissionRequired("Please sign in to view calendar events");
            return;
        }
        if (!account.getGrantedScopes().contains(new Scope(CalendarScopes.CALENDAR_READONLY))) {
            callback.onPermissionRequired("Calendar access not granted. Please grant calendar permissions in app settings.");
            return;
        }

        // Setup calendar service
        if (!setupCalendarService(account)) {
            callback.onError("Failed to setup calendar service");
            return;
        }
        executorService.execute(() -> {
            try {
                List<CalendarEvent> events = fetchEventsFromAPI(daysAhead);
                callback.onEventsLoaded(events);
            } catch (Exception e) {
                Log.e(TAG, "Error loading calendar events", e);
                callback.onError("Failed to load calendar events: " + e.getMessage());
            }
        });
    }

    private boolean setupCalendarService(GoogleSignInAccount account) {
        try {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    Collections.singleton(CalendarScopes.CALENDAR_READONLY)
            );
            credential.setSelectedAccount(account.getAccount());

            calendarService = new Calendar.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            )
                    .setApplicationName("TwinMind")
                    .build();

            Log.d(TAG, "Calendar service initialized successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error setting up calendar service", e);
            return false;
        }
    }

    private List<CalendarEvent> fetchEventsFromAPI(int daysAhead) throws Exception {

        long now = System.currentTimeMillis();
        long futureTime = now + (daysAhead * 24L * 60 * 60 * 1000);

        com.google.api.client.util.DateTime timeMin = new com.google.api.client.util.DateTime(now);
        com.google.api.client.util.DateTime timeMax = new com.google.api.client.util.DateTime(futureTime);

        Events events = calendarService.events().list("primary")
                .setTimeMin(timeMin)
                .setTimeMax(timeMax)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setMaxResults(50)
                .execute();

        List<Event> items = events.getItems();
        List<CalendarEvent> calendarEvents = new ArrayList<>();

        if (items != null) {
            for (Event event : items) {
                CalendarEvent calendarEvent = new CalendarEvent(event);
                calendarEvents.add(calendarEvent);
                Log.d(TAG, "Loaded event: " + calendarEvent.title + " at " + calendarEvent.startTime);
            }
        }

        Log.d(TAG, "Successfully loaded " + calendarEvents.size() + " events");
        return calendarEvents;
    }

    public void refreshEvents(CalendarCallback callback) {
        loadUpcomingEvents(callback);
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    public boolean hasCalendarPermission() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null &&
                account.getGrantedScopes().contains(new Scope(CalendarScopes.CALENDAR_READONLY));
    }

    public boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(context) != null;
    }
}