package com.example.twinmind;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_ASSISTANT = 2;
    private static final int TYPE_SYSTEM = 3;

    private List<ChatMessage> messages;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatMessagesAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        switch (message.type) {
            case USER: return TYPE_USER;
            case ASSISTANT: return TYPE_ASSISTANT;
            case SYSTEM: return TYPE_SYSTEM;
            default: return TYPE_ASSISTANT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_USER:
                return new UserMessageViewHolder(inflater.inflate(R.layout.item_chat_user, parent, false));
            case TYPE_ASSISTANT:
                return new AssistantMessageViewHolder(inflater.inflate(R.layout.item_chat_assistant, parent, false));
            case TYPE_SYSTEM:
                return new SystemMessageViewHolder(inflater.inflate(R.layout.item_chat_system, parent, false));
            default:
                return new AssistantMessageViewHolder(inflater.inflate(R.layout.item_chat_assistant, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message, timeFormat);
        } else if (holder instanceof AssistantMessageViewHolder) {
            ((AssistantMessageViewHolder) holder).bind(message, timeFormat);
        } else if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder classes
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
        }

        void bind(ChatMessage message, SimpleDateFormat timeFormat) {
            messageText.setText(message.content);
            timeText.setText(timeFormat.format(new Date(message.timestamp)));
        }
    }

    static class AssistantMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        AssistantMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
        }

        void bind(ChatMessage message, SimpleDateFormat timeFormat) {
            messageText.setText(message.content);
            timeText.setText(timeFormat.format(new Date(message.timestamp)));
        }
    }

    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        SystemMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.content);
        }
    }
}