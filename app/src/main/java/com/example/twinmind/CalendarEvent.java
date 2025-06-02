package com.example.twinmind;

import com.google.api.services.calendar.model.Event;
import java.util.Date;

public class CalendarEvent {
    public String title;
    public Date startTime;
    public Date endTime;
    public String location;
    public boolean isAllDay;
    public String description;
    public String eventId;

    public CalendarEvent(Event event) {
        this.eventId = event.getId();
        this.title = event.getSummary() != null ? event.getSummary() : "No title";
        this.location = event.getLocation();
        this.description = event.getDescription();

        // Handle different time formats
        if (event.getStart().getDateTime() != null) {
            this.startTime = new Date(event.getStart().getDateTime().getValue());
            this.isAllDay = false;
        } else if (event.getStart().getDate() != null) {
            this.startTime = new Date(event.getStart().getDate().getValue());
            this.isAllDay = true;
        }

        if (event.getEnd().getDateTime() != null) {
            this.endTime = new Date(event.getEnd().getDateTime().getValue());
        } else if (event.getEnd().getDate() != null) {
            this.endTime = new Date(event.getEnd().getDate().getValue());
        }
    }

    // Constructor for manual creation (useful for testing)
    public CalendarEvent(String title, Date startTime, Date endTime, String location, boolean isAllDay) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.isAllDay = isAllDay;
        this.eventId = "manual_" + System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "CalendarEvent{" +
                "title='" + title + '\'' +
                ", startTime=" + startTime +
                ", isAllDay=" + isAllDay +
                ", location='" + location + '\'' +
                '}';
    }
}