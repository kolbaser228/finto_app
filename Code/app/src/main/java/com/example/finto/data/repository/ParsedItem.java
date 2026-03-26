package com.example.finto.data.repository;

import com.google.gson.annotations.SerializedName;

public class ParsedItem {
    public double amount;
    public String comment;
    @SerializedName("category_id")
    public Long categoryId;
    public Long date;
    public ParsedItem() {
    }

    public ParsedItem(double amount, String comment, Long categoryId, Long date) {
        this.amount = amount;
        this.comment = comment;
        this.categoryId = categoryId;
        this.date = date;
    }
}