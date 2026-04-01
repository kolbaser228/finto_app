package com.example.finto.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllTransactionsFragment extends Fragment {

    private RecyclerView rvAllTransactions;
    private TransactionAdapter adapter;
    private ImageView btnBack;
    
    private TransactionDao transactionDao;
    private Map<Long, Category> categoryMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_transactions, container, false);

        btnBack = view.findViewById(R.id.btn_back);
        rvAllTransactions = view.findViewById(R.id.rv_all_transactions);
        rvAllTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TransactionAdapter(new ArrayList<>(), categoryMap);
        rvAllTransactions.setAdapter(adapter);

        transactionDao = AppDatabase.getDatabase(requireContext()).transactionDao();

        // Повернення назад за допомогою диспетчера або фрагмент-менеджера
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        transactionDao.getAllCategories().observe(getViewLifecycleOwner(), new Observer<List<Category>>() {
            @Override
            public void onChanged(List<Category> categories) {
                categoryMap.clear();
                if (categories != null) {
                    for (Category cat : categories) {
                        categoryMap.put(cat.id, cat);
                    }
                }
                observeAllTransactions();
            }
        });

        return view;
    }

    private void observeAllTransactions() {
        transactionDao.getAllTransactions().observe(getViewLifecycleOwner(), new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactions) {
                // Передаємо всі наявні транзакції без обмеження в 5 штук
                adapter.updateData(transactions != null ? transactions : new ArrayList<>(), categoryMap);
            }
        });
    }
}
