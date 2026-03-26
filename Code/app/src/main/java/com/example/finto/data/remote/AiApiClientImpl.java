package com.example.finto.data.remote;

import com.example.finto.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiApiClientImpl implements AiApiClient {

    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent";
    private final OkHttpClient client;

    public AiApiClientImpl() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(45, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .build();
    }

    public String analyzeImageAndText(String prompt, String base64Image) throws Exception {
        JSONObject jsonBody = new JSONObject();

        JSONArray contents = new JSONArray();
        JSONObject contentObj = new JSONObject();
        JSONArray parts = new JSONArray();

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);
        parts.put(textPart);

        JSONObject inlineDataPart = new JSONObject();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Image);
        inlineDataPart.put("inline_data", inlineData);
        parts.put(inlineDataPart);

        contentObj.put("parts", parts);
        contents.put(contentObj);
        jsonBody.put("contents", contents);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("response_mime_type", "application/json");
        jsonBody.put("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .header("x-goog-api-key", API_KEY)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Невідома помилка";
                throw new IOException("Помилка Gemini: " + errorBody);
            }

            String responseData = response.body().string();
            JSONObject jsonObject = new JSONObject(responseData);
            return jsonObject.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        }
    }
}