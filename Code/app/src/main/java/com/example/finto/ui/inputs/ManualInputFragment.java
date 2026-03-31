package com.example.finto.ui.inputs;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finto.R;
import com.example.finto.data.local.AppDatabase;
import com.example.finto.data.local.Category;
import com.example.finto.data.local.Transaction;
import com.example.finto.data.local.TransactionDao;

import java.util.ArrayList;
import java.util.List;

public class ManualInputFragment extends Fragment {

    private EditText etAmount;
    private Spinner spinnerCategory;
    private Button btnCreateCategory;
    private Button btnSave;
    private TextView tvTitle;

    private AppDatabase db;
    private TransactionDao dao;

    private final List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    public ManualInputFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manual_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etAmount = view.findViewById(R.id.etAmount);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        btnCreateCategory = view.findViewById(R.id.btnCreateCategory);
        btnSave = view.findViewById(R.id.btnSave);
        tvTitle = view.findViewById(R.id.tvManualAddTitle);

        db = AppDatabase.getDatabase(requireContext());
        dao = db.transactionDao();

        loadCategories();

        btnCreateCategory.setOnClickListener(v -> showCreateCategoryDialog());
        btnSave.setOnClickListener(v -> saveManualTransaction());
    }

    private void loadCategories() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = dao.getAllCategoriesSync();

            categoryList.clear();
            categoryList.addAll(categories);

            List<String> categoryNames = new ArrayList<>();
            for (Category category : categoryList) {
                categoryNames.add(category.name);
            }

            requireActivity().runOnUiThread(() -> {
                spinnerAdapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        categoryNames
                );
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(spinnerAdapter);
            });
        });
    }

    private void showCreateCategoryDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Enter category name");

        new AlertDialog.Builder(requireContext())
                .setTitle("Create New Category")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String categoryName = input.getText().toString().trim();

                    if (TextUtils.isEmpty(categoryName)) {
                        Toast.makeText(requireContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createNewCategory(categoryName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createNewCategory(String categoryName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (Category existingCategory : categoryList) {
                if (existingCategory.name.equalsIgnoreCase(categoryName)) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "This category already exists", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
            }

            Category newCategory = new Category(categoryName, "ic_other");
            long newId = dao.insertCategory(newCategory);
            newCategory.id = newId;

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Category created", Toast.LENGTH_SHORT).show();
                loadCategories();
            });
        });
    }

    private void saveManualTransaction() {
        String amountText = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(amountText)) {
            Toast.makeText(requireContext(), "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "No categories available", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerCategory.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= categoryList.size()) {
            Toast.makeText(requireContext(), "Select category", Toast.LENGTH_SHORT).show();
            return;
        }

        Category selectedCategory = categoryList.get(selectedPosition);

        Transaction transaction = new Transaction(
                amount,
                "Manual add",
                System.currentTimeMillis(),
                false,
                selectedCategory.id
        );

        AppDatabase.databaseWriteExecutor.execute(() -> {
            dao.insertTransaction(transaction);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Saved successfully", Toast.LENGTH_SHORT).show();
                etAmount.setText("");
                spinnerCategory.setSelection(0);
            });
        });
    }
}