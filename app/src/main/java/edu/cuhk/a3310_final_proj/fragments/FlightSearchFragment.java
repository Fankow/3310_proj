package edu.cuhk.a3310_final_proj.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.FlightAdapter;
import edu.cuhk.a3310_final_proj.models.Flight;
import edu.cuhk.a3310_final_proj.network.FlightSearchClient;

public class FlightSearchFragment extends Fragment {

    private EditText etDepartureAirport, etArrivalAirport;
    private TextView tvDepartureDate, tvReturnDate;
    private RadioGroup rgTripType;
    private RadioButton rbOneWay, rbRoundTrip;
    private Button btnSearch;
    private ProgressBar progressBar;
    private TextView tvNoFlights, tvOutboundHeader, tvReturnHeader;
    private RecyclerView rvOutboundFlights, rvReturnFlights;

    private FlightSearchClient flightSearchClient;
    private FlightAdapter outboundAdapter;
    private FlightAdapter returnAdapter;

    private List<Flight> outboundFlights = new ArrayList<>();
    private List<Flight> returnFlights = new ArrayList<>();

    private Calendar departureDateCalendar = Calendar.getInstance();
    private Calendar returnDateCalendar = Calendar.getInstance();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flight_search, container, false);

        // Initialize UI components
        etDepartureAirport = view.findViewById(R.id.et_departure_airport);
        etArrivalAirport = view.findViewById(R.id.et_arrival_airport);
        tvDepartureDate = view.findViewById(R.id.tv_departure_date);
        tvReturnDate = view.findViewById(R.id.tv_return_date);
        rgTripType = view.findViewById(R.id.rg_trip_type);
        rbOneWay = view.findViewById(R.id.rb_one_way);
        rbRoundTrip = view.findViewById(R.id.rb_round_trip);
        btnSearch = view.findViewById(R.id.btn_search);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoFlights = view.findViewById(R.id.tv_no_flights);
        tvOutboundHeader = view.findViewById(R.id.tv_outbound_header);
        tvReturnHeader = view.findViewById(R.id.tv_return_header);
        rvOutboundFlights = view.findViewById(R.id.rv_outbound_flights);
        rvReturnFlights = view.findViewById(R.id.rv_return_flights);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up RecyclerViews
        rvOutboundFlights.setLayoutManager(new LinearLayoutManager(requireContext()));
        outboundAdapter = new FlightAdapter(requireContext(), outboundFlights);
        rvOutboundFlights.setAdapter(outboundAdapter);

        rvReturnFlights.setLayoutManager(new LinearLayoutManager(requireContext()));
        returnAdapter = new FlightAdapter(requireContext(), returnFlights);
        rvReturnFlights.setAdapter(returnAdapter);

        // Initialize client
        flightSearchClient = FlightSearchClient.getInstance();

        // Set default dates (today for departure, tomorrow for return)
        returnDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvDepartureDate.setText(dateFormat.format(departureDateCalendar.getTime()));
        tvReturnDate.setText(dateFormat.format(returnDateCalendar.getTime()));

        setupDatePickers();
        setupTripTypeSelection();
        setupSearchButton();
    }

    private void setupDatePickers() {
        // Date picker for departure date
        tvDepartureDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        departureDateCalendar.set(Calendar.YEAR, year);
                        departureDateCalendar.set(Calendar.MONTH, month);
                        departureDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        tvDepartureDate.setText(dateFormat.format(departureDateCalendar.getTime()));

                        // If return date is before departure date, update it
                        if (returnDateCalendar.before(departureDateCalendar)) {
                            returnDateCalendar.setTime(departureDateCalendar.getTime());
                            returnDateCalendar.add(Calendar.DAY_OF_MONTH, 1);
                            tvReturnDate.setText(dateFormat.format(returnDateCalendar.getTime()));
                        }
                    },
                    departureDateCalendar.get(Calendar.YEAR),
                    departureDateCalendar.get(Calendar.MONTH),
                    departureDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Date picker for return date
        tvReturnDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        returnDateCalendar.set(Calendar.YEAR, year);
                        returnDateCalendar.set(Calendar.MONTH, month);
                        returnDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        tvReturnDate.setText(dateFormat.format(returnDateCalendar.getTime()));
                    },
                    returnDateCalendar.get(Calendar.YEAR),
                    returnDateCalendar.get(Calendar.MONTH),
                    returnDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(departureDateCalendar.getTimeInMillis());
            datePickerDialog.show();
        });
    }

    private void setupTripTypeSelection() {
        // Trip type selection
        rgTripType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_one_way) {
                tvReturnDate.setEnabled(false);
                tvReturnDate.setAlpha(0.5f);
            } else {
                tvReturnDate.setEnabled(true);
                tvReturnDate.setAlpha(1.0f);
            }
        });

        // Default to one-way
        rbOneWay.setChecked(true);
        tvReturnDate.setEnabled(false);
        tvReturnDate.setAlpha(0.5f);
    }

    private void setupSearchButton() {
        btnSearch.setOnClickListener(v -> {
            String departureAirport = etDepartureAirport.getText().toString().trim();
            String arrivalAirport = etArrivalAirport.getText().toString().trim();
            String departureDate = tvDepartureDate.getText().toString().trim();
            String returnDate = tvReturnDate.getText().toString().trim();

            if (departureAirport.isEmpty() || arrivalAirport.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter airport codes", Toast.LENGTH_SHORT).show();
                return;
            }

            // Clear previous results
            outboundFlights.clear();
            returnFlights.clear();
            outboundAdapter.notifyDataSetChanged();
            returnAdapter.notifyDataSetChanged();

            showLoading(true);

            if (rbOneWay.isChecked()) {
                // One-way search
                flightSearchClient.searchFlights(
                        departureAirport.toUpperCase(),
                        arrivalAirport.toUpperCase(),
                        departureDate,
                        new FlightSearchClient.FlightSearchCallback() {
                    @Override
                    public void onSuccess(List<Flight> flights) {
                        showLoading(false);
                        if (flights.isEmpty()) {
                            showNoFlightsMessage(true);
                        } else {
                            showNoFlightsMessage(false);
                            outboundFlights.addAll(flights);
                            outboundAdapter.notifyDataSetChanged();

                            // Show only outbound section
                            tvOutboundHeader.setVisibility(View.VISIBLE);
                            rvOutboundFlights.setVisibility(View.VISIBLE);
                            tvReturnHeader.setVisibility(View.GONE);
                            rvReturnFlights.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        showError(errorMessage);
                    }
                }
                );
            } else {
                // Round-trip search
                flightSearchClient.searchRoundTripFlights(
                        departureAirport.toUpperCase(),
                        arrivalAirport.toUpperCase(),
                        departureDate,
                        returnDate,
                        new FlightSearchClient.RoundTripFlightSearchCallback() {
                    @Override
                    public void onSuccess(List<Flight> outbound, List<Flight> returnFlights) {
                        showLoading(false);
                        if (outbound.isEmpty() && returnFlights.isEmpty()) {
                            showNoFlightsMessage(true);
                        } else {
                            showNoFlightsMessage(false);

                            // Show outbound section
                            if (!outbound.isEmpty()) {
                                outboundFlights.addAll(outbound);
                                outboundAdapter.notifyDataSetChanged();
                                tvOutboundHeader.setVisibility(View.VISIBLE);
                                rvOutboundFlights.setVisibility(View.VISIBLE);
                            } else {
                                tvOutboundHeader.setVisibility(View.GONE);
                                rvOutboundFlights.setVisibility(View.GONE);
                            }

                            // Show return section
                            if (!returnFlights.isEmpty()) {
                                FlightSearchFragment.this.returnFlights.addAll(returnFlights);
                                returnAdapter.notifyDataSetChanged();
                                tvReturnHeader.setVisibility(View.VISIBLE);
                                rvReturnFlights.setVisibility(View.VISIBLE);
                            } else {
                                tvReturnHeader.setVisibility(View.GONE);
                                rvReturnFlights.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        showError(errorMessage);
                    }
                }
                );
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSearch.setEnabled(!isLoading);
    }

    private void showNoFlightsMessage(boolean show) {
        tvNoFlights.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}
