package edu.cuhk.a3310_final_proj.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.HotelAdapter;
import edu.cuhk.a3310_final_proj.models.Hotel;
import edu.cuhk.a3310_final_proj.network.HotelSearchClient;

public class HotelSearchFragment extends Fragment {

    private static final String TAG = "HotelSearchFragment";

    private TextInputEditText etQuery;
    private TextInputEditText etCheckInDate;
    private TextInputEditText etCheckOutDate;
    private Button btnSearch;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private List<Hotel> hotels = new ArrayList<>();
    private HotelAdapter hotelAdapter;

    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hotel_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        etQuery = view.findViewById(R.id.et_hotel_query);
        etCheckInDate = view.findViewById(R.id.et_check_in_date);
        etCheckOutDate = view.findViewById(R.id.et_check_out_date);
        btnSearch = view.findViewById(R.id.btn_search_hotels);
        recyclerView = view.findViewById(R.id.rv_hotels);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        hotelAdapter = new HotelAdapter(requireContext(), hotels);
        recyclerView.setAdapter(hotelAdapter);

        // Set up date pickers
        setupDatePickers();

        // Set up search button
        btnSearch.setOnClickListener(v -> searchHotels());
    }

    private void setupDatePickers() {
        // Check-in date picker
        etCheckInDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year1, monthOfYear, dayOfMonth);
                        etCheckInDate.setText(displayDateFormat.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });

        // Check-out date picker
        etCheckOutDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year1, monthOfYear, dayOfMonth);
                        etCheckOutDate.setText(displayDateFormat.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void searchHotels() {
        String query = etQuery.getText().toString().trim();
        String checkInDateStr = etCheckInDate.getText().toString().trim();
        String checkOutDateStr = etCheckOutDate.getText().toString().trim();

        if (query.isEmpty()) {
            etQuery.setError("Please enter a hotel name or location");
            return;
        }

        if (checkInDateStr.isEmpty()) {
            etCheckInDate.setError("Please select check-in date");
            return;
        }

        if (checkOutDateStr.isEmpty()) {
            etCheckOutDate.setError("Please select check-out date");
            return;
        }

        try {
            Calendar checkInCal = Calendar.getInstance();
            Calendar checkOutCal = Calendar.getInstance();

            checkInCal.setTime(displayDateFormat.parse(checkInDateStr));
            checkOutCal.setTime(displayDateFormat.parse(checkOutDateStr));

            if (checkInCal.after(checkOutCal)) {
                etCheckOutDate.setError("Check-out date must be after check-in date");
                return;
            }

            // Format dates for API
            String checkInDate = apiDateFormat.format(checkInCal.getTime());
            String checkOutDate = apiDateFormat.format(checkOutCal.getTime());

            // Perform search
            performHotelSearch(query, checkInDate, checkOutDate);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing dates", e);
            Toast.makeText(requireContext(), "Error parsing dates", Toast.LENGTH_SHORT).show();
        }
    }

    private void performHotelSearch(String location, String checkInDate, String checkOutDate) {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        // Clear previous results
        hotels.clear();

        // Get hotel search client
        HotelSearchClient searchClient = HotelSearchClient.getInstance();

        // Perform search
        searchClient.searchHotels(location, checkInDate, checkOutDate, new HotelSearchClient.HotelSearchCallback() {
            @Override
            public void onSuccess(List<Hotel> results) {
                progressBar.setVisibility(View.GONE);

                if (results.isEmpty()) {
                    tvEmptyState.setText("No hotels found");
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    hotels.addAll(results);
                    hotelAdapter.notifyDataSetChanged();
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvEmptyState.setText("Error: " + errorMessage);
                tvEmptyState.setVisibility(View.VISIBLE);
                Log.e(TAG, "Hotel search error: " + errorMessage);
            }
        });
    }
}
