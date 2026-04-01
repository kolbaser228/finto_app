package com.example.finto.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finto.R;
import com.example.finto.data.local.AppDatabase;
import com.example.finto.data.local.Category;
import com.example.finto.data.local.Transaction;
import com.example.finto.data.local.TransactionDao;
import com.example.finto.ui.inputs.ManualInputFragment;
import com.example.finto.ui.inputs.OcrScannerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private TextView tvTotalBalance;
    private TextView tvBalancePercentage;
    private TextView tvViewAll;
    private LinearLayout btnManualAdd, btnScanReceipt;
    
    private TransactionDao transactionDao;
    private Map<Long, Category> categoryMap = new HashMap<>();
    private List<Transaction> recentTransactions = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvTotalBalance = view.findViewById(R.id.tv_total_balance);
        tvBalancePercentage = view.findViewById(R.id.tv_balance_percentage);
        rvTransactions = view.findViewById(R.id.rv_recent_transactions);
        tvViewAll = view.findViewById(R.id.tv_view_all);
        btnManualAdd = view.findViewById(R.id.btn_manual_add);
        btnScanReceipt = view.findViewById(R.id.btn_scan_receipt);

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTransactions.setNestedScrollingEnabled(false); // To scroll smoothly inside ScrollView

        adapter = new TransactionAdapter(new ArrayList<>(), categoryMap);
        rvTransactions.setAdapter(adapter);

        transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao();

        // Спостерігаємо за загальним балансом
        transactionDao.getTotalBalance().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double balance) {
                if (balance == null) balance = 0.0;
                tvTotalBalance.setText(String.format(Locale.US, "$%,.2f", balance));
            }
        });

        // Спостерігаємо за категоріями для перекладу іконок
        transactionDao.getAllCategories().observe(getViewLifecycleOwner(), new Observer<List<Category>>() {
            @Override
            public void onChanged(List<Category> categories) {
                categoryMap.clear();
                if (categories != null) {
                    for (Category cat : categories) {
                        categoryMap.put(cat.id, cat);
                    }
                }
                adapter.updateData(recentTransactions, categoryMap);
            }
        });

        // Спостерігаємо за транзакціями для оновлення списку та відсотків
        observeTransactions();

        // Click listeners for Hybrid Input buttons
        btnManualAdd.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ManualInputFragment())
                    .addToBackStack(null)
                    .commit();
            
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
            if(nav != null) nav.setSelectedItemId(R.id.nav_manual_input);
        });

        btnScanReceipt.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new OcrScannerFragment())
                    .addToBackStack(null)
                    .commit();
                    
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
            if(nav != null) nav.setSelectedItemId(R.id.nav_ocr_input);
        });

        tvViewAll.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AllTransactionsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void observeTransactions() {
        transactionDao.getAllTransactions().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                // Беремо останні 5 транзакцій для Dashboard та рахуємо загальні доходи/витрати
                recentTransactions.clear();
                double totalIncome = 0;
                double totalExpense = 0;

                if (transactions != null) {
                    for (int i = 0; i < transactions.size(); i++) {
                        Transaction t = transactions.get(i);
                        if (i < 5) {
                            recentTransactions.add(t);
                        }
                        if (t.amount > 0) {
                            totalIncome += t.amount;
                        } else {
                            totalExpense += Math.abs(t.amount);
                        }
                    }
                }
                adapter.updateData(recentTransactions, categoryMap);

                // Відображення відсотка витрат
                if (tvBalancePercentage != null) {
                    if (totalIncome > 0) {
                        double percent = (totalExpense / totalIncome) * 100.0;
                        tvBalancePercentage.setVisibility(View.VISIBLE);
                        tvBalancePercentage.setText(String.format(Locale.US, "%.1f%% of income spent", percent));
                        if (percent > 80) {
                            tvBalancePercentage.setTextColor(android.graphics.Color.parseColor("#E53935")); // Red
                        } else if (percent > 50) {
                            tvBalancePercentage.setTextColor(android.graphics.Color.parseColor("#FB8C00")); // Orange
                        } else {
                            tvBalancePercentage.setTextColor(android.graphics.Color.parseColor("#388E3C")); // Green
                        }
                    } else if (totalExpense > 0) {
                        tvBalancePercentage.setVisibility(View.VISIBLE);
                        tvBalancePercentage.setText("Only expenses recorded");
                        tvBalancePercentage.setTextColor(android.graphics.Color.parseColor("#E53935"));
                    } else {
                        tvBalancePercentage.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
}
