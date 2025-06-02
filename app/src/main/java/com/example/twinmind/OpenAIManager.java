package com.example.twinmind;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAIManager {

    private static final String TAG = "OpenAIManager";

    // Updated to use current Gemini model name
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";
    private static final String API_KEY = "AIzaSyATcaLbPkSCMa0S3YiIFocBZh5biRuHuko"; // Your Google API key

    private ExecutorService executorService;
    private Context context;

    public OpenAIManager(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
    }

    public interface OpenAICallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void generateText(String prompt, OpenAICallback callback) {
        Log.d(TAG, "Starting Gemini text generation request");

        executorService.execute(() -> {
            try {
                String response = callGeminiAPI(prompt);
                callback.onSuccess(response);
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                callback.onError(e.getMessage());
            }
        });
    }

    private String callGeminiAPI(String prompt) throws Exception {
        URL url = new URL(GEMINI_API_URL + API_KEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Set request properties
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        // Create request body for Gemini API
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        // Add generation config for better results
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 2048);
        requestBody.put("generationConfig", generationConfig);

        Log.d(TAG, "Sending request to Gemini API with model: gemini-2.0-flash");

        // Send request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        // Read response
        int responseCode = connection.getResponseCode();
        Log.d(TAG, "Gemini API Response Code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Log.d(TAG, "Gemini API response: " + response.toString());

            // Parse Gemini response
            JSONObject responseJson = new JSONObject(response.toString());

            if (responseJson.has("candidates")) {
                JSONArray candidates = responseJson.getJSONArray("candidates");

                if (candidates.length() > 0) {
                    JSONObject candidate = candidates.getJSONObject(0);
                    content = candidate.getJSONObject("content");
                    parts = content.getJSONArray("parts");
                    String text = parts.getJSONObject(0).getString("text");

                    Log.d(TAG, "Gemini response parsed successfully");
                    return text;
                } else {
                    throw new Exception("No candidates in Gemini response");
                }
            } else {
                throw new Exception("Invalid Gemini response format");
            }
        } else {
            // Read error response
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();

            Log.e(TAG, "Gemini API Error: " + errorResponse.toString());

            // Handle specific error cases
            if (responseCode == 400) {
                throw new Exception("Bad request - check your prompt");
            } else if (responseCode == 403) {
                throw new Exception("API key issue or Generative Language API not enabled");
            } else if (responseCode == 429) {
                throw new Exception("Rate limit exceeded");
            } else {
                throw new Exception("Gemini API error: " + responseCode + " - " + errorResponse.toString());
            }
        }
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}