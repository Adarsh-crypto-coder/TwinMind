package com.example.twinmind;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TranscriptionDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "TranscriptionDB";
    private static final String DATABASE_NAME = "twinmind_transcriptions.db";
    private static final int DATABASE_VERSION = 2; // Increment version for schema change

    // Table names
    private static final String TABLE_RECORDING_SESSIONS = "recording_sessions";
    private static final String TABLE_TRANSCRIPTIONS = "transcriptions";

    // Recording Sessions table columns
    private static final String COLUMN_SESSION_ID = "session_id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_START_TIME = "start_time";
    private static final String COLUMN_END_TIME = "end_time";
    private static final String COLUMN_DURATION = "duration";
    private static final String COLUMN_LOCATION = "location";

    // Transcriptions table columns
    private static final String COLUMN_TRANSCRIPTION_TEXT = "transcription_text";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_CHUNK_INDEX = "chunk_index";

    private static TranscriptionDatabaseHelper instance;

    public static synchronized TranscriptionDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new TranscriptionDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private TranscriptionDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Drop old tables if they exist
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSCRIPTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDING_SESSIONS);

        // Create new tables
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {
        // Create recording sessions table
        String createSessionsTable = "CREATE TABLE " + TABLE_RECORDING_SESSIONS + " (" +
                COLUMN_SESSION_ID + " TEXT PRIMARY KEY, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_START_TIME + " INTEGER, " +
                COLUMN_END_TIME + " INTEGER, " +
                COLUMN_DURATION + " INTEGER, " +
                COLUMN_LOCATION + " TEXT" +
                ")";

        // Create transcriptions table
        String createTranscriptionsTable = "CREATE TABLE " + TABLE_TRANSCRIPTIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SESSION_ID + " TEXT, " +
                COLUMN_TRANSCRIPTION_TEXT + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_CHUNK_INDEX + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_SESSION_ID + ") REFERENCES " +
                TABLE_RECORDING_SESSIONS + "(" + COLUMN_SESSION_ID + ")" +
                ")";

        db.execSQL(createSessionsTable);
        db.execSQL(createTranscriptionsTable);

        Log.d(TAG, "Tables created successfully");
    }

    // Recording Session methods
    public void createRecordingSession(String sessionId, String title, long startTime, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SESSION_ID, sessionId);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_START_TIME, startTime);
        values.put(COLUMN_LOCATION, location);

        long result = db.insert(TABLE_RECORDING_SESSIONS, null, values);

        if (result != -1) {
            Log.d(TAG, "Recording session created: " + sessionId);
        } else {
            Log.e(TAG, "Failed to create recording session: " + sessionId);
        }
    }

    public void endRecordingSession(String sessionId, long endTime, long duration) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_END_TIME, endTime);
        values.put(COLUMN_DURATION, duration);

        int rowsAffected = db.update(TABLE_RECORDING_SESSIONS, values,
                COLUMN_SESSION_ID + " = ?", new String[]{sessionId});

        if (rowsAffected > 0) {
            Log.d(TAG, "Recording session ended: " + sessionId);
        } else {
            Log.e(TAG, "Failed to end recording session: " + sessionId);
        }
    }

    public RecordingSession getRecordingSession(String sessionId) {
        SQLiteDatabase db = this.getReadableDatabase();
        RecordingSession session = null;

        Cursor cursor = db.query(TABLE_RECORDING_SESSIONS, null,
                COLUMN_SESSION_ID + " = ?", new String[]{sessionId},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            session = new RecordingSession();
            session.sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID));
            session.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
            session.startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIME));
            session.endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_END_TIME));
            session.duration = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
            session.location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION));

            cursor.close();
        }

        return session;
    }

    // Transcription methods using TranscriptionEntry
    public void insertTranscription(String sessionId, String transcriptionText, long timestamp, int chunkIndex) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_SESSION_ID, sessionId);
        values.put(COLUMN_TRANSCRIPTION_TEXT, transcriptionText);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_CHUNK_INDEX, chunkIndex);

        long result = db.insert(TABLE_TRANSCRIPTIONS, null, values);

        if (result != -1) {
            Log.d(TAG, "Transcription inserted for session: " + sessionId + ", chunk: " + chunkIndex);
        } else {
            Log.e(TAG, "Failed to insert transcription for session: " + sessionId);
        }
    }

    public List<TranscriptionEntry> getTranscriptionsForSession(String sessionId) {
        List<TranscriptionEntry> transcriptions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_TRANSCRIPTIONS, null,
                COLUMN_SESSION_ID + " = ?", new String[]{sessionId},
                null, null, COLUMN_CHUNK_INDEX + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                TranscriptionEntry entry = new TranscriptionEntry();
                entry.sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID));
                entry.transcriptionText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TRANSCRIPTION_TEXT));
                entry.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                entry.chunkIndex = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHUNK_INDEX));

                transcriptions.add(entry);
            }
            cursor.close();
        }

        Log.d(TAG, "Retrieved " + transcriptions.size() + " transcriptions for session: " + sessionId);
        return transcriptions;
    }

    public List<RecordingSession> getAllRecordingSessions() {
        List<RecordingSession> sessions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RECORDING_SESSIONS, null, null, null,
                null, null, COLUMN_START_TIME + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                RecordingSession session = new RecordingSession();
                session.sessionId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID));
                session.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                session.startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_START_TIME));
                session.endTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_END_TIME));
                session.duration = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
                session.location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION));

                sessions.add(session);
            }
            cursor.close();
        }

        return sessions;
    }

    public void deleteRecordingSession(String sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete transcriptions first (due to foreign key)
        db.delete(TABLE_TRANSCRIPTIONS, COLUMN_SESSION_ID + " = ?", new String[]{sessionId});

        // Delete recording session
        int rowsAffected = db.delete(TABLE_RECORDING_SESSIONS, COLUMN_SESSION_ID + " = ?", new String[]{sessionId});

        if (rowsAffected > 0) {
            Log.d(TAG, "Recording session deleted: " + sessionId);
        } else {
            Log.e(TAG, "Failed to delete recording session: " + sessionId);
        }
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSCRIPTIONS, null, null);
        db.delete(TABLE_RECORDING_SESSIONS, null, null);
        Log.d(TAG, "All data cleared");
    }

    public List<SessionSummary> getAllSessionSummaries() {
        List<SessionSummary> sessions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT session_id, " +
                "MIN(timestamp) as start_time, " +
                "MAX(timestamp) as end_time, " +
                "COUNT(*) as transcription_count, " +
                "GROUP_CONCAT(transcription_text, ' ') as all_text " +
                "FROM transcriptions " +
                "GROUP BY session_id " +
                "ORDER BY start_time DESC";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String sessionId = cursor.getString(cursor.getColumnIndexOrThrow("session_id"));
                long startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"));
                long endTime = cursor.getLong(cursor.getColumnIndexOrThrow("end_time"));
                int transcriptionCount = cursor.getInt(cursor.getColumnIndexOrThrow("transcription_count"));
                String allText = cursor.getString(cursor.getColumnIndexOrThrow("all_text"));

                // Get first meaningful transcription
                String firstTranscription = getFirstMeaningfulTranscription(allText);

                SessionSummary session = new SessionSummary(
                        sessionId,
                        startTime,
                        endTime,
                        firstTranscription,
                        transcriptionCount
                );

                sessions.add(session);

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return sessions;
    }

    private String getFirstMeaningfulTranscription(String allText) {
        if (allText == null || allText.trim().isEmpty()) {
            return "Recording session";
        }

        // Split by spaces and take first meaningful words
        String[] words = allText.trim().split("\\s+");
        StringBuilder firstTranscription = new StringBuilder();

        for (String word : words) {
            if (firstTranscription.length() + word.length() > 100) {
                break;
            }
            if (firstTranscription.length() > 0) {
                firstTranscription.append(" ");
            }
            firstTranscription.append(word);
        }

        return firstTranscription.toString();
    }
}