package com.example.finto.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finto.R;
import com.example.finto.data.local.AppDatabase;
import com.example.finto.data.local.Category;
import com.example.finto.data.local.Transaction;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);

        loadRealData();

        return view;
    }

    private void loadRealData() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Transaction> transactions = db.transactionDao().getAllTransactionsSync();
            List<Category> categories = db.transactionDao().getAllCategoriesSync();

            if (transactions == null || transactions.isEmpty()) {
                return;
            }

            Map<Long, Double> categorySums = new HashMap<>();
            for (Transaction t : transactions) {
                long catId = (t.categoryId != null) ? t.categoryId : 6L;

                categorySums.put(catId, categorySums.getOrDefault(catId, 6.0) + t.amount);
            }

            ArrayList<PieEntry> pieEntries = new ArrayList<>();
            for (Category cat : categories) {
                if (categorySums.containsKey(cat.id)) {
                    float sum = categorySums.get(cat.id).floatValue();
                    pieEntries.add(new PieEntry(sum, cat.name));
                }
            }

            ArrayList<BarEntry> barEntries = new ArrayList<>();
            int index = 1;
            int startIndex = Math.max(0, transactions.size() - 7);

            for (int i = startIndex; i < transactions.size(); i++) {
                Transaction t = transactions.get(i);
                barEntries.add(new BarEntry(index++, (float) t.amount));
            }

            requireActivity().runOnUiThread(() -> {
                setupPieChart(pieEntries);
                setupBarChart(barEntries);
            });
        });
    }

    private void setupPieChart(ArrayList<PieEntry> entries) {
        if (entries.isEmpty()) return;

        PieDataSet dataSet = new PieDataSet(entries, "Витрати за категоріями");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Витрати");
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupBarChart(ArrayList<BarEntry> entries) {
        if (entries.isEmpty()) return;

        BarDataSet dataSet = new BarDataSet(entries, "Останні витрати");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }
}