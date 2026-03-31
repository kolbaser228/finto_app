package com.example.finto.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insertTransaction(Transaction transaction);

    @Insert
    void insertTransactions(List<Transaction> transactions);

    @Insert
    void insertCategories(List<Category> categories);

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT * FROM transactions ORDER BY date ASC")
    List<Transaction> getAllTransactionsSync();

    @Query("SELECT SUM(amount) FROM transactions")
    LiveData<Double> getTotalBalance();

    @Query("SELECT * FROM categories")
    List<Category> getAllCategoriesSync();
}