package com.example.twinmind;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarEventsAdapter extends RecyclerView.Adapter<CalendarEventsAdapter.EventViewHolder> {

    private List<CalendarEvent> events;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
    private OnEventClickListener eventClickListener;

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    public CalendarEventsAdapter(List<CalendarEvent> events) {
        this.events = events;
    }

    public CalendarEventsAdapter(List<CalendarEvent> events, OnEventClickListener listener) {
        this.events = events;
        this.eventClickListener = listener;
    }

    public void setEvents(List<CalendarEvent> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.eventClickListener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = events.get(position);
        holder.bind(event, dateFormat, timeFormat, dayFormat);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (eventClickListener != null) {
                eventClickListener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDate;
        private TextView tvDay;
        private TextView tvTime;
        private TextView tvTitle;
        private TextView tvLocation;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_event_date);
            tvDay = itemView.findViewById(R.id.tv_event_day);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
        }

        public void bind(CalendarEvent event, SimpleDateFormat dateFormat,
                         SimpleDateFormat timeFormat, SimpleDateFormat dayFormat) {

            if (event.startTime != null) {
                // Format date and day
                tvDate.setText(formatDateForDisplay(event.startTime, dateFormat));
                tvDay.setText(dayFormat.format(event.startTime));

                // Format time
                if (event.isAllDay) {
                    tvTime.setText("All day");
                } else {
                    String timeText = timeFormat.format(event.startTime);

                    // Add end time if it's the same day and not all day
                    if (event.endTime != null && isSameDay(event.startTime, event.endTime)) {
                        timeText += " - " + timeFormat.format(event.endTime);
                    }

                    tvTime.setText(timeText);
                }
            }

            // Set title
            tvTitle.setText(event.title);

            // Set location (show/hide based on availability)
            if (event.location != null && !event.location.trim().isEmpty()) {
                tvLocation.setText(event.location);
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }
        }

        private String formatDateForDisplay(Date date, SimpleDateFormat dateFormat) {
            Calendar cal = Calendar.getInstance();
            Calendar today = Calendar.getInstance();
            cal.setTime(date);

            // Check if it's today
            if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                return "Today";
            }

            // Check if it's tomorrow
            today.add(Calendar.DAY_OF_YEAR, 1);
            if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                return "Tomorrow";
            }

            // Use formatted date
            return dateFormat.format(date);
        }

        private boolean isSameDay(Date date1, Date date2) {
            Calendar cal1 = Calendar.getInstance();
            Calendar cal2 = Calendar.getInstance();
            cal1.setTime(date1);
            cal2.setTime(date2);

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
    }
}