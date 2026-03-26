package com.example.finto.data.repository;

import com.example.finto.data.local.AppDatabase;
import com.example.finto.data.local.Category;
import com.example.finto.data.local.Transaction;
import com.example.finto.data.local.TransactionDao;
import com.example.finto.data.remote.AiApiClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.List;

public class OcrRepository {
    private final TransactionDao transactionDao;
    private final AiApiClient aiApiClient;

    public OcrRepository(TransactionDao transactionDao, AiApiClient aiApiClient) {
        this.transactionDao = transactionDao;
        this.aiApiClient = aiApiClient;
    }

    public List<ParsedItem> analyzeReceiptImage(String base64Image) throws Exception {
        List<Category> categories = transactionDao.getAllCategoriesSync();
        StringBuilder categoriesPromptContext = new StringBuilder();
        for (Category cat : categories) {
            categoriesPromptContext.append(cat.id).append(": ").append(cat.name).append("\n");
        }

        String prompt = "Ти — фінансовий асистент. Уважно подивися на це зображення:\n" +
                "1. Перевір, чи це дійсно касовий чек.\n" +
                "2. Якщо це НЕ чек, поверни: {\"is_receipt\": false, \"error_message\": \"Це не схоже на чек.\"}\n" +
                "3. Якщо це чек, витягни всі покупки.\n" +
                "УВАГА: ТИ ПОВИНЕН обов'язково призначити кожній покупці 'category_id', використовуючи ТІЛЬКИ цифри з цього списку:\n" +
                categoriesPromptContext.toString() + "\n" +
                "Заборонено вигадувати інші ID! Якщо жодна категорія не підходить ідеально, використовуй ID категорії 'Other'.\n" +
                "Поверни суворий JSON у такому форматі:\n" +
                "{\"is_receipt\": true, \"transactions\": [{ \"amount\": 10.5, \"comment\": \"Назва товару\", \"category_id\": 1, \"date\": 1697880000000 }]}\n";

        String aiResponseJson = aiApiClient.analyzeImageAndText(prompt, base64Image);

        Gson gson = new Gson();
        AiResponse parsedResponse = gson.fromJson(aiResponseJson, AiResponse.class);

        if (!parsedResponse.is_receipt || parsedResponse.transactions == null) {
            String errorMsg = parsedResponse.error_message != null ? parsedResponse.error_message : "Текст не розпізнано як чек.";
            throw new Exception(errorMsg);
        }

        return parsedResponse.transactions;
    }

    public void saveTransactions(List<ParsedItem> items) {
        // Знаходимо ID категорії "Other"
        Long fallbackCategoryId = transactionDao.getAllCategoriesSync().stream()
                .filter(c -> c.name.equalsIgnoreCase("Other"))
                .map(c -> c.id)
                .findFirst()
                .orElse(1L); // Якщо Other не знайдено, ставимо 1 (Food)

        List<Transaction> transactionsToSave = new ArrayList<>();
        for (ParsedItem item : items) {
            long dateToSave = (item.date != null) ? item.date : System.currentTimeMillis();

            Long finalCategoryId = (item.categoryId > 0) ? item.categoryId : fallbackCategoryId;

            transactionsToSave.add(new Transaction(item.amount, item.comment, dateToSave, true, finalCategoryId));
        }
        transactionDao.insertTransactions(transactionsToSave);
    }

    private List<ParsedItem> parseJsonToTransactions(String json) {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<ParsedItem>>(){}.getType();

        try {
            return gson.fromJson(json, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}