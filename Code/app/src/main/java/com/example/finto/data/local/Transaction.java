package com.example.finto.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        foreignKeys = @ForeignKey(
                entity = Category.class,
                parentColumns = "id",
                childColumns = "category_id",
                onDelete = ForeignKey.SET_NULL
        )
)
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public double amount;
    public String comment;
    public long date;
    public boolean created_by_ocr;

    @ColumnInfo(name = "category_id")
    public Long categoryId;

    public Transaction(double amount, String comment, long date, boolean created_by_ocr, Long categoryId) {
        this.amount = amount;
        this.comment = comment;
        this.date = date;
        this.created_by_ocr = created_by_ocr;
        this.categoryId = categoryId;
    }
}