package com.example.twinmind;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CalendarUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("EEEE", Locale.getDefault());
    private static final SimpleDateFormat FULL_FORMAT = new SimpleDateFormat("EEEE, MMM d, yyyy 'at' h:mm a", Locale.getDefault());

    /**
     * Format date for display in different contexts
     */
    public static String formatDateRelative(Date date) {
        if (date == null) return "Unknown date";

        Calendar cal = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        cal.setTime(date);
        if (isSameDay(cal, today)) {
            return "Today";
        }
        today.add(Calendar.DAY_OF_YEAR, 1);
        if (isSameDay(cal, today)) {
            return "Tomorrow";
        }
        today.add(Calendar.DAY_OF_YEAR, -2);
        if (isSameDay(cal, today)) {
            return "Yesterday";
        }
        today = Calendar.getInstance();
        if (isThisWeek(cal, today)) {
            return DAY_FORMAT.format(date);
        }

        return DATE_FORMAT.format(date);
    }

    /**
     * Format time for display
     */
    public static String formatTime(Date startTime, Date endTime, boolean isAllDay) {
        if (startTime == null) return "Unknown time";

        if (isAllDay) {
            return "All day";
        }

        String timeText = TIME_FORMAT.format(startTime);
        if (endTime != null && isSameDay(startTime, endTime)) {
            timeText += " - " + TIME_FORMAT.format(endTime);
        }

        return timeText;
    }

    /**
     * Format full event time for details
     */
    public static String formatFullEventTime(Date startTime, Date endTime, boolean isAllDay) {
        if (startTime == null) return "Unknown time";

        if (isAllDay) {
            return formatDateRelative(startTime) + " (All day)";
        }

        String result = FULL_FORMAT.format(startTime);
        if (endTime != null) {
            if (!isSameDay(startTime, endTime)) {
                result += " - " + FULL_FORMAT.format(endTime);
            } else {
                long diffMinutes = (endTime.getTime() - startTime.getTime()) / (1000 * 60);
                if (diffMinutes > 30) {
                    result += " - " + TIME_FORMAT.format(endTime);
                }
            }
        }

        return result;
    }

    /**
     * Get event duration as human readable string
     */
    public static String getEventDuration(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            return "Unknown duration";
        }

        long diffMs = endTime.getTime() - startTime.getTime();

        if (diffMs <= 0) {
            return "Instant";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60;

        StringBuilder duration = new StringBuilder();

        if (days > 0) {
            duration.append(days).append(days == 1 ? " day" : " days");
        }

        if (hours > 0) {
            if (duration.length() > 0) duration.append(" ");
            duration.append(hours).append(hours == 1 ? " hour" : " hours");
        }

        if (minutes > 0 && days == 0) { // Only show minutes if less than a day
            if (duration.length() > 0) duration.append(" ");
            duration.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }

        return duration.length() > 0 ? duration.toString() : "Less than a minute";
    }

    /**
     * Check if event is happening soon (within next 2 hours)
     */
    public static boolean isEventSoon(Date startTime) {
        if (startTime == null) return false;

        long now = System.currentTimeMillis();
        long eventTime = startTime.getTime();
        long diffMs = eventTime - now;

        return diffMs > 0 && diffMs <= (2 * 60 * 60 * 1000); // 2 hours in milliseconds
    }

    /**
     * Check if event is currently happening
     */
    public static boolean isEventNow(Date startTime, Date endTime) {
        if (startTime == null) return false;

        long now = System.currentTimeMillis();
        long startMs = startTime.getTime();

        if (endTime != null) {
            long endMs = endTime.getTime();
            return now >= startMs && now <= endMs;
        } else {
            return now >= startMs && now <= (startMs + 60 * 60 * 1000);
        }
    }

    /**
     * Get time until event starts
     */
    public static String getTimeUntilEvent(Date startTime) {
        if (startTime == null) return "Unknown";

        long now = System.currentTimeMillis();
        long eventTime = startTime.getTime();
        long diffMs = eventTime - now;

        if (diffMs <= 0) {
            return "Started";
        }

        long minutes = diffMs / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return "in " + days + (days == 1 ? " day" : " days");
        } else if (hours > 0) {
            return "in " + hours + (hours == 1 ? " hour" : " hours");
        } else if (minutes > 0) {
            return "in " + minutes + (minutes == 1 ? " minute" : " minutes");
        } else {
            return "starting now";
        }
    }

    /**
     * Check if two dates are the same day
     */
    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        return isSameDay(cal1, cal2);
    }

    /**
     * Check if two calendars are the same day
     */
    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Check if date is within this week
     */
    private static boolean isThisWeek(Calendar eventCal, Calendar today) {
        Calendar startOfWeek = (Calendar) today.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);

        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_WEEK, 6);
        endOfWeek.set(Calendar.HOUR_OF_DAY, 23);
        endOfWeek.set(Calendar.MINUTE, 59);
        endOfWeek.set(Calendar.SECOND, 59);
        endOfWeek.set(Calendar.MILLISECOND, 999);

        return eventCal.after(startOfWeek) && eventCal.before(endOfWeek);
    }

    /**
     * Create time range for API calls
     */
    public static class TimeRange {
        public final long startTime;
        public final long endTime;

        public TimeRange(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public com.google.api.client.util.DateTime getStartDateTime() {
            return new com.google.api.client.util.DateTime(startTime);
        }

        public com.google.api.client.util.DateTime getEndDateTime() {
            return new com.google.api.client.util.DateTime(endTime);
        }
    }

    /**
     * Get time range for next N days
     */
    public static TimeRange getTimeRangeForDays(int days) {
        long now = System.currentTimeMillis();
        long future = now + (days * 24L * 60 * 60 * 1000);
        return new TimeRange(now, future);
    }

    /**
     * Get time range for current week
     */
    public static TimeRange getTimeRangeForThisWeek() {
        Calendar now = Calendar.getInstance();

        Calendar startOfWeek = (Calendar) now.clone();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);

        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_WEEK, 6);
        endOfWeek.set(Calendar.HOUR_OF_DAY, 23);
        endOfWeek.set(Calendar.MINUTE, 59);
        endOfWeek.set(Calendar.SECOND, 59);
        endOfWeek.set(Calendar.MILLISECOND, 999);

        return new TimeRange(startOfWeek.getTimeInMillis(), endOfWeek.getTimeInMillis());
    }

    /**
     * Check if event conflicts with another event
     */
    public static boolean eventsConflict(CalendarEvent event1, CalendarEvent event2) {
        if (event1 == null || event2 == null ||
                event1.startTime == null || event2.startTime == null) {
            return false;
        }

        long start1 = event1.startTime.getTime();
        long end1 = event1.endTime != null ? event1.endTime.getTime() : start1 + (60 * 60 * 1000); // Default 1 hour

        long start2 = event2.startTime.getTime();
        long end2 = event2.endTime != null ? event2.endTime.getTime() : start2 + (60 * 60 * 1000); // Default 1 hour

        return start1 < end2 && start2 < end1;
    }
}