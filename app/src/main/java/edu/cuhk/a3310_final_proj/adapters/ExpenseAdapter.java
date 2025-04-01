// New file: app/src/main/java/edu/cuhk/a3310_final_proj/adapters/ExpenseAdapter.java
package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.models.Expense;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private Context context;
    private ExpenseAdapterListener listener;

    public interface ExpenseAdapterListener {

        void onViewExpense(Expense expense, int position);

        void onEditExpense(Expense expense, int position);

        void onDeleteExpense(Expense expense, int position);
    }

    public ExpenseAdapter(Context context, ExpenseAdapterListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.bind(expense, position);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses.clear();
        if (expenses != null) {
            this.expenses.addAll(expenses);
        }
        notifyDataSetChanged();
    }

    public void addExpense(Expense expense) {
        this.expenses.add(expense);
        notifyItemInserted(expenses.size() - 1);
    }

    public void removeExpense(int position) {
        this.expenses.remove(position);
        notifyItemRemoved(position);
    }

    public void updateExpense(Expense expense, int position) {
        this.expenses.set(position, expense);
        notifyItemChanged(position);
    }

    public List<Expense> getExpenses() {
        return new ArrayList<>(expenses);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        TextView tvExpenseAmount, tvExpenseCategory, tvExpenseDate;
        ImageView ivReceiptImage;
        ImageButton btnView, btnEdit, btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExpenseAmount = itemView.findViewById(R.id.tv_expense_amount);
            tvExpenseCategory = itemView.findViewById(R.id.tv_expense_category);
            tvExpenseDate = itemView.findViewById(R.id.tv_expense_date);
            ivReceiptImage = itemView.findViewById(R.id.iv_receipt_thumbnail);
            btnView = itemView.findViewById(R.id.btn_view_expense);
            btnEdit = itemView.findViewById(R.id.btn_edit_expense);
            btnDelete = itemView.findViewById(R.id.btn_delete_expense);
        }

        public void bind(Expense expense, int position) {
            // Format amount with currency
            String formattedAmount = String.format(Locale.getDefault(),
                    "%s %.2f", expense.getCurrency(), expense.getAmount());
            tvExpenseAmount.setText(formattedAmount);

            tvExpenseCategory.setText(expense.getCategory());

            // Format date
            if (expense.getDate() != null) {
                tvExpenseDate.setText(android.text.format.DateFormat.format("MMM dd, yyyy", expense.getDate()));
            } else {
                tvExpenseDate.setText("No date");
            }

            // Load receipt image thumbnail if available
            if (expense.getReceiptImageUrl() != null && !expense.getReceiptImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(expense.getReceiptImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(ivReceiptImage);
                ivReceiptImage.setVisibility(View.VISIBLE);
            } else {
                ivReceiptImage.setVisibility(View.GONE);
            }

            // Set click listeners
            btnView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewExpense(expense, position);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditExpense(expense, position);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteExpense(expense, position);
                }
            });
        }
    }
}
