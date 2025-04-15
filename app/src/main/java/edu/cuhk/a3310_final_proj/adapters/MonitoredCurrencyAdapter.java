package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cuhk.a3310_final_proj.R;

public class MonitoredCurrencyAdapter extends RecyclerView.Adapter<MonitoredCurrencyAdapter.ViewHolder> {

    private List<String> currencies;
    private Map<String, Double> rates;
    private Map<String, Double> convertedValues;
    private Map<String, String> currencyNames;
    private Context context;
    private OnCurrencyRemoveListener removeListener;

    public interface OnCurrencyRemoveListener {

        void onCurrencyRemove(String currency);
    }

    public MonitoredCurrencyAdapter(Context context, List<String> currencies, OnCurrencyRemoveListener removeListener) {
        this.context = context;
        this.currencies = currencies;
        this.rates = new HashMap<>();
        this.convertedValues = new HashMap<>();
        this.removeListener = removeListener;
        initCurrencyNames();
    }

    private void initCurrencyNames() {
        currencyNames = new HashMap<>();
        currencyNames.put("USD", "US Dollar");
        currencyNames.put("EUR", "Euro");
        currencyNames.put("GBP", "British Pound");
        currencyNames.put("JPY", "Japanese Yen");
        currencyNames.put("AUD", "Australian Dollar");
        currencyNames.put("CAD", "Canadian Dollar");
        currencyNames.put("CHF", "Swiss Franc");
        currencyNames.put("CNY", "Chinese Yuan");
        currencyNames.put("HKD", "Hong Kong Dollar");
        currencyNames.put("SGD", "Singapore Dollar");
    }

    public void updateRates(Map<String, Double> rates) {
        this.rates = rates;
        notifyDataSetChanged();
    }

    public void updateRatesAndValues(Map<String, Double> rates, Map<String, Double> convertedValues) {
        this.rates = rates;
        this.convertedValues = convertedValues;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_monitored_currency, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String currency = currencies.get(position);

        holder.currencyCode.setText(currency);
        holder.currencyName.setText(currencyNames.containsKey(currency)
                ? currencyNames.get(currency) : "Unknown Currency");


        if (convertedValues.containsKey(currency)) {

            holder.currencyRate.setText(String.format("%.2f", convertedValues.get(currency)));
        } else if (rates.containsKey(currency)) {
            holder.currencyRate.setText(String.format("%.4f", rates.get(currency)));
        } else {
            holder.currencyRate.setText("Loading...");
        }

        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onCurrencyRemove(currency);
            }
        });
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView currencyCode, currencyName, currencyRate;
        ImageButton removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            currencyCode = itemView.findViewById(R.id.currency_code);
            currencyName = itemView.findViewById(R.id.currency_name);
            currencyRate = itemView.findViewById(R.id.currency_rate);
            removeButton = itemView.findViewById(R.id.remove_button);
        }
    }
}
