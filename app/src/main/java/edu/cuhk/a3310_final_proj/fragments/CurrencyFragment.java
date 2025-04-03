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
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.gson.annotations.SerializedName;
import edu.cuhk.a3310_final_proj.network.ExchangeRateResponse;
import edu.cuhk.a3310_final_proj.network.CurrencyConverter;
import edu.cuhk.a3310_final_proj.LoginActivity;

import java.util.*;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import edu.cuhk.a3310_final_proj.R;

public class CurrencyFragment extends Fragment {

    private static final String TAG = "CurrencyFragment";
    private Spinner fromSpinner, toSpinner;
    private EditText amountInput;
    private TextView resultText;
    private FirebaseFirestore db;
    private String userId;
    private List<String> favoriteCurrencies = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private Call<edu.cuhk.a3310_final_proj.network.ExchangeRateResponse> activeCall;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_currency, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeFirebase();
        initializeUI(view);
        loadUserPreferences();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            navigateToLoginWithMessage("Session expired, please login again");
            return;
        }

        userId = user.getUid();
    }

    private void initializeUI(View view) {
        fromSpinner = view.findViewById(R.id.fromCurrencySpinner);
        toSpinner = view.findViewById(R.id.toCurrencySpinner);
        amountInput = view.findViewById(R.id.amountInput);
        resultText = view.findViewById(R.id.resultText);

        // Initialize spinner with default values
        List<String> defaultCurrencies = Arrays.asList("USD", "HKD", "EUR", "GBP", "JPY");
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>(defaultCurrencies)
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fromSpinner.setAdapter(spinnerAdapter);
        toSpinner.setAdapter(spinnerAdapter);

        // Set up button click listeners
        view.findViewById(R.id.convertButton).setOnClickListener(v -> performConversion());
        view.findViewById(R.id.historyButton).setOnClickListener(v -> showConversionHistory());
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
                                updateSpinnersWithPreferences(prefs);
                            }
                        }
                    } else {
                        Log.w(TAG, "Error loading preferences", task.getException());
                        Toast.makeText(requireContext(), "Failed to load preferences", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSpinnersWithPreferences(Map<String, Object> prefs) {
        String baseCurrency = (String) prefs.get("baseCurrency");
        List<String> favorites = (List<String>) prefs.get("favoriteCurrencies");

        if (favorites != null) {
            favoriteCurrencies.clear();
            favoriteCurrencies.addAll(favorites);
            updateSpinnerOrder(baseCurrency);
        }
    }

    private void updateSpinnerOrder(String baseCurrency) {
        List<String> orderedCurrencies = new ArrayList<>(favoriteCurrencies);
        for (String currency : Arrays.asList("USD", "HKD", "EUR", "GBP", "JPY")) {
            if (!orderedCurrencies.contains(currency)) {
                orderedCurrencies.add(currency);
            }
        }

        spinnerAdapter.clear();
        spinnerAdapter.addAll(orderedCurrencies);

        if (baseCurrency != null) {
            int position = orderedCurrencies.indexOf(baseCurrency);
            if (position >= 0) {
                fromSpinner.setSelection(position);
            }
        }
    }

    private void performConversion() {
        String amountStr = amountInput.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String from = fromSpinner.getSelectedItem().toString();
            String to = toSpinner.getSelectedItem().toString();
            double amount = Double.parseDouble(amountStr);

            activeCall = CurrencyConverter.convert(
                    from,
                    to,
                    amount,
                    false,
                    new CurrencyConverter.Callback() {
                @Override
                public void onSuccess(double result) {
                    requireActivity().runOnUiThread(() -> {
                        resultText.setText(String.format("%.2f %s = %.2f %s",
                                amount, from, result, to));
                        saveConversionHistory(from, to, amount, result);
                    });
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(()
                            -> Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show());
                }
            }
            );

        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Conversion failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveConversionHistory(String from, String to, double amount, double result) {
        if (userId == null) {
            return;
        }

        Map<String, Object> conversion = new HashMap<>();
        conversion.put("from", from);
        conversion.put("to", to);
        conversion.put("amount", amount);
        conversion.put("result", result);
        conversion.put("timestamp", new Date());

        db.collection("users").document(userId)
                .update("conversionHistory", FieldValue.arrayUnion(conversion))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Conversion saved to history"))
                .addOnFailureListener(e -> {
                    handleFirestoreError(e);
                    Log.w(TAG, "Error saving conversion history", e);
                });
    }

    private void showConversionHistory() {
        // Implement navigation to history fragment
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ConversionHistoryFragment())
                .addToBackStack("currency_history")
                .commit();
    }

    private void handleFirestoreError(Exception e) {
        Log.w(TAG, "Firestore operation failed", e);

        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) e;
            if (firestoreEx.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                navigateToLoginWithMessage("Session expired, please login again");
            }
        }
    }

    private void navigateToLoginWithMessage(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Authentication Required")
                .setMessage(message)
                .setPositiveButton("Login", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (activeCall != null && !activeCall.isCanceled()) {
            activeCall.cancel();
            Log.d(TAG, "Network request cancelled");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        spinnerAdapter = null;
    }
}
