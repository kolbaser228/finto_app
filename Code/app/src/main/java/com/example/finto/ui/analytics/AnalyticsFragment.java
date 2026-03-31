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

public class AnalyticsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analytics, container, false);

        PieChart pieChart = view.findViewById(R.id.pieChart);
        BarChart barChart = view.findViewById(R.id.barChart);

        setupPieChart(pieChart);
        setupBarChart(barChart);

        return view;
    }

    private void setupPieChart(PieChart pieChart) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(42f, "Rent"));
        entries.add(new PieEntry(28f, "Food"));
        entries.add(new PieEntry(15f, "Bills"));

        PieDataSet dataSet = new PieDataSet(entries, "Expenses");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.animateY(1000);
    }

    private void setupBarChart(BarChart barChart) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, 100));
        entries.add(new BarEntry(2, 150));
        entries.add(new BarEntry(3, 80));
        entries.add(new BarEntry(4, 240));

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Spend");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.animateY(1000);
    }
}
