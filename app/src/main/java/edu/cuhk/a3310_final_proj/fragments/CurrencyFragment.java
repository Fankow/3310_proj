package edu.cuhk.a3310_final_proj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cuhk.a3310_final_proj.BuildConfig;
import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.MonitoredCurrencyAdapter;
import edu.cuhk.a3310_final_proj.network.CurrencyConverter;
import edu.cuhk.a3310_final_proj.network.ExchangeRateResponse;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class CurrencyFragment extends Fragment {

    private static final String TAG = "CurrencyFragment";
    private Spinner sourceCurrencySpinner;
    private EditText amountInput;
    private Button convertButton;
    private FirebaseFirestore db;
    private String userId;
    private Call<ExchangeRateResponse> activeCall;

    private RecyclerView monitoredCurrenciesRecycler;
    private MonitoredCurrencyAdapter monitoredAdapter;
    private Button addMonitoredCurrencyButton;
    private List<String> monitoredCurrencies = new ArrayList<>();
    private Map<String, Double> currentRates = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_currency, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        initializeUI(view);
        loadUserPreferences();
    }

    private void initializeUI(View view) {
        // Initialize UI components
        sourceCurrencySpinner = view.findViewById(R.id.sourceCurrencySpinner);
        amountInput = view.findViewById(R.id.amountInput);
        convertButton = view.findViewById(R.id.convertButton);

        // Setup convert button click listener
        convertButton.setOnClickListener(v -> performConversion());

        // Initialize monitored currencies recyclerView
        monitoredCurrenciesRecycler = view.findViewById(R.id.monitored_currencies_recycler);
        monitoredCurrenciesRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        monitoredAdapter = new MonitoredCurrencyAdapter(requireContext(), monitoredCurrencies,
                currency -> removeMonitoredCurrency(currency));
        monitoredCurrenciesRecycler.setAdapter(monitoredAdapter);

        addMonitoredCurrencyButton = view.findViewById(R.id.add_monitored_currency_button);
        addMonitoredCurrencyButton.setOnClickListener(v -> showAddCurrencyDialog());

        // Initialize spinner with common currencies
        List<String> currencies = Arrays.asList("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "HKD", "SGD");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, currencies);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceCurrencySpinner.setAdapter(spinnerAdapter);

        // Set default value
        sourceCurrencySpinner.setSelection(currencies.indexOf("USD"));
    }

    private void loadUserPreferences() {
        if (userId == null) {
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> prefs = (Map<String, Object>) document.get("preferences");
                            if (prefs != null) {
                                // Load spinner default selection if saved
                                String defaultSource = (String) prefs.get("defaultSourceCurrency");
                                if (defaultSource != null) {
                                    int position = ((ArrayAdapter) sourceCurrencySpinner.getAdapter()).getPosition(defaultSource);
                                    if (position >= 0) {
                                        sourceCurrencySpinner.setSelection(position);
                                    }
                                }

                                // Load monitored currencies
                                List<String> monitored = (List<String>) prefs.get("monitoredCurrencies");
                                if (monitored != null && !monitored.isEmpty()) {
                                    monitoredCurrencies.clear();
                                    monitoredCurrencies.addAll(monitored);
                                    monitoredAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Error loading preferences", task.getException());
                        Toast.makeText(requireContext(), "Failed to load preferences", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performConversion() {
        String sourceCurrency = sourceCurrencySpinner.getSelectedItem().toString();
        String amountStr = amountInput.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (monitoredCurrencies.isEmpty()) {
            Toast.makeText(requireContext(), "Please add currencies to monitor", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancel previous call if active
        if (activeCall != null) {
            activeCall.cancel();
        }

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CurrencyConverter.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ExchangeRateService service = retrofit.create(ExchangeRateService.class);
        activeCall = service.getRates(sourceCurrency, CurrencyConverter.API_KEY);

        Toast.makeText(requireContext(), "Converting...", Toast.LENGTH_SHORT).show();

        activeCall.enqueue(new retrofit2.Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Rates received successfully");
                    currentRates = response.body().getRates();

                    // Create a map to store the converted values
                    Map<String, Double> convertedValues = new HashMap<>();

                    // Calculate converted values for each monitored currency
                    for (String currency : monitoredCurrencies) {
                        Double rate = currentRates.get(currency);
                        if (rate != null) {
                            double convertedAmount = amount * rate;
                            convertedValues.put(currency, convertedAmount);
                        }
                    }

                    // Update the adapter with both rates and converted values
                    monitoredAdapter.updateRatesAndValues(currentRates, convertedValues);

                    // Save this conversion to history
                    saveConversionToHistory(sourceCurrency, monitoredCurrencies, amount, convertedValues);
                } else {
                    Log.e(TAG, "Failed to load rates: " + response.code());
                    Toast.makeText(requireContext(), "Failed to load rates", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    Log.e(TAG, "Network error", t);
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveConversionToHistory(String from, List<String> toList, double amount, Map<String, Double> results) {
        if (userId == null) {
            return;
        }

        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("fromCurrency", from);
        historyItem.put("amount", amount);
        historyItem.put("timestamp", System.currentTimeMillis());

        // Save the list of target currencies and their converted values
        historyItem.put("toCurrencies", toList);
        historyItem.put("results", results);

        db.collection("users").document(userId)
                .collection("conversionHistory")
                .add(historyItem)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Conversion saved with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding conversion", e));
    }

    private void showAddCurrencyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Currency to Monitor");

        // Create a list of available currencies (excluding already monitored ones)
        List<String> availableCurrencies = new ArrayList<>(Arrays.asList(
                "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "HKD", "SGD"
        ));
        availableCurrencies.removeAll(monitoredCurrencies);

        if (availableCurrencies.isEmpty()) {
            Toast.makeText(requireContext(), "All currencies are already monitored", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] currencyArray = availableCurrencies.toArray(new String[0]);

        builder.setItems(currencyArray, (dialog, which) -> {
            String selectedCurrency = currencyArray[which];
            addMonitoredCurrency(selectedCurrency);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addMonitoredCurrency(String currency) {
        if (!monitoredCurrencies.contains(currency)) {
            monitoredCurrencies.add(currency);
            monitoredAdapter.notifyItemInserted(monitoredCurrencies.size() - 1);
            saveMonitoredCurrencies();
        }
    }

    private void removeMonitoredCurrency(String currency) {
        int position = monitoredCurrencies.indexOf(currency);
        if (position != -1) {
            monitoredCurrencies.remove(position);
            monitoredAdapter.notifyItemRemoved(position);
            saveMonitoredCurrencies();
        }
    }

    private void saveMonitoredCurrencies() {
        if (userId == null) {
            return;
        }

        // First check if the document exists
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> preferences = new HashMap<>();
                    preferences.put("monitoredCurrencies", monitoredCurrencies);
                    preferences.put("defaultSourceCurrency", sourceCurrencySpinner.getSelectedItem().toString());

                    if (documentSnapshot.exists()) {
                        // Document exists - update it
                        db.collection("users").document(userId)
                                .update("preferences", preferences)
                                .addOnSuccessListener(aVoid
                                        -> Log.d(TAG, "Monitored currencies saved successfully"))
                                .addOnFailureListener(e -> {
                                    handleFirestoreError(e);
                                    Log.w(TAG, "Error saving monitored currencies", e);
                                });
                    } else {
                        // Document doesn't exist - create it
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("preferences", preferences);
                        userData.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());

                        db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid
                                        -> Log.d(TAG, "User profile created with monitored currencies"))
                                .addOnFailureListener(e -> {
                                    handleFirestoreError(e);
                                    Log.w(TAG, "Error creating user profile", e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    handleFirestoreError(e);
                    Log.w(TAG, "Error loading user document", e);
                });
    }

    private void handleFirestoreError(Exception e) {
        Toast.makeText(requireContext(), "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    // Interface for currency rate API
    private interface ExchangeRateService {

        @GET("{base}")
        Call<ExchangeRateResponse> getRates(
                @Path("base") String baseCurrency,
                @Query("api_key") String apiKey
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (activeCall != null) {
            activeCall.cancel();
        }
    }
}
