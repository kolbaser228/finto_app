package com.example.finto.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Category.class, Transaction.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "finto_database")
                            .addCallback(sRoomDatabaseCallback) // ДОДАНО: підключаємо наш колбек
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            databaseWriteExecutor.execute(() -> {
                TransactionDao dao = INSTANCE.transactionDao();

                List<Category> defaultCategories = Arrays.asList(
                        new Category("Food", "ic_food"),
                        new Category("Rent", "ic_rent"),
                        new Category("Bills", "ic_bills"),
                        new Category("Transport", "ic_transport"),
                        new Category("Electronics", "ic_electronics"),
                        new Category("Other", "ic_other")
                );

                dao.insertCategories(defaultCategories);

                // Отримуємо згенеровані ID категорій, щоб прив'язати транзакції
                long time = System.currentTimeMillis();
                List<Transaction> dummyTransactions = Arrays.asList(
                    // Початковий бюджет, щоб після витрат баланс був рівно $3000.00, як на макеті! (3000 + 187.25 = 3187.25)
                    new Transaction(3187.25, "Initial Deposit", time - 86400000L * 10, false, 6L), // 6 is Other / Income
                    
                    new Transaction(-15.50, "Groceries", time, false, 1L), // Food -> Shopping cart
                    new Transaction(-42.00, "Transport card", time, false, 4L), // Transport -> Bus
                    new Transaction(-125.00, "Electricity", time, false, 3L), // Bills -> Utility Bill
                    new Transaction(-4.75, "Espresso", time, false, 1L) // Food -> Coffee cup
                );
                dao.insertTransactions(dummyTransactions);
            });
        }
    };
}