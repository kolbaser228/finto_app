package com.example.finto.data.remote;

public interface AiApiClient {
    /**
     * Відправляє згенерований промпт (текст з чека + категорії) до ШІ
     * та повертає структуровану відповідь у форматі JSON.
     */
    String analyzeImageAndText(String prompt, String base64Image) throws Exception;
}