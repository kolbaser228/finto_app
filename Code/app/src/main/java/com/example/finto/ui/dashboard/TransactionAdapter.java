package com.example.finto.ui.dashboard;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finto.R;
import com.example.finto.data.local.Category;
import com.example.finto.data.local.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> transactions;
    private Map<Long, Category> categoryMap;
    // Format according to wireframe: "Oct 21, 2023"
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    public TransactionAdapter(List<Transaction> transactions, Map<Long, Category> categoryMap) {
        this.transactions = transactions;
        this.categoryMap = categoryMap;
    }

    public void updateData(List<Transaction> transactions, Map<Long, Category> categoryMap) {
        this.transactions = transactions;
        this.categoryMap = categoryMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        if (categoryMap != null && transaction.categoryId != null) {
            Category category = categoryMap.get(transaction.categoryId);
            if (category != null) {
                holder.tvComment.setText(category.name);
            } else {
                holder.tvComment.setText("Unknown");
            }
        } else {
            holder.tvComment.setText("Unknown");
        }

        holder.tvDate.setText(dateFormat.format(new Date(transaction.date)));
        
        double absAmount = Math.abs(transaction.amount);
        if (transaction.amount >= 0) {
            holder.tvAmount.setText(String.format(Locale.US, "+$%.2f", absAmount));
            holder.tvAmount.setTextColor(Color.parseColor("#388E3C")); // green
        } else {
            holder.tvAmount.setText(String.format(Locale.US, "-$%.2f", absAmount));
            holder.tvAmount.setTextColor(Color.parseColor("#E53935")); // red
        }

        if (categoryMap != null && transaction.categoryId != null) {
            Category category = categoryMap.get(transaction.categoryId);
            if (category != null) {
                // Видалимо текст категорії, так як у макеті залишаємо лише Title та Date
                holder.tvCategory.setVisibility(View.GONE);
                
                int iconResId = android.R.drawable.ic_menu_agenda;
                if (category.icon != null) {
                    switch (category.icon) {
                        case "ic_food": iconResId = android.R.drawable.ic_menu_gallery; break;
                        case "ic_rent": iconResId = android.R.drawable.ic_menu_compass; break;
                        case "ic_bills": iconResId = android.R.drawable.ic_menu_info_details; break;
                        case "ic_transport": iconResId = android.R.drawable.ic_menu_directions; break;
                        case "ic_electronics": iconResId = android.R.drawable.ic_menu_camera; break;
                    }
                }
                holder.ivIcon.setImageResource(iconResId);
            } else {
                holder.tvCategory.setVisibility(View.GONE);
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
            }
        } else {
            holder.tvCategory.setVisibility(View.GONE);
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        }
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvComment, tvCategory, tvDate, tvAmount;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvComment = itemView.findViewById(R.id.tv_transaction_comment);
            tvCategory = itemView.findViewById(R.id.tv_transaction_category);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
        }
    }
}
