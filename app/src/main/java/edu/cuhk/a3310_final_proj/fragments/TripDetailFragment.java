package edu.cuhk.a3310_final_proj.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.PropertyName;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.DayItineraryAdapter;
import edu.cuhk.a3310_final_proj.adapters.DocumentAdapter;
import edu.cuhk.a3310_final_proj.adapters.ExpenseAdapter;
import edu.cuhk.a3310_final_proj.models.DayItineraryItem;
import edu.cuhk.a3310_final_proj.models.Location;
import edu.cuhk.firebase.FirestoreManager;
import edu.cuhk.a3310_final_proj.models.Expense;
import edu.cuhk.a3310_final_proj.models.Trip;

public class TripDetailFragment extends Fragment implements OnMapReadyCallback {

    private static final int PICK_EXPENSE_RECEIPT_REQUEST = 3;

    private MapView mapView;
    private GoogleMap googleMap;
    private List<Marker> locationMarkers = new ArrayList<>();

    private String tripId;
    private FirestoreManager firestoreManager;
    private Trip currentTrip;

    private String trip_id;

    @PropertyName("trip_id")
    public String getTripId() {
        return trip_id;
    }

    @PropertyName("trip_id")
    public void setTripId(String tripId) {
        this.trip_id = tripId;
    }

    // UI elements
    private ImageView ivTripImage;
    private TextView tvTripName, tvDestination, tvDateRange, tvBudget, tvFlightNumber, tvNotes;
    private RecyclerView rvDayContainers, rvDocuments, rvExpenses;
    private Button btnEditTrip, btnDeleteTrip, btnAddExpense;
    private View emptyStateView;

    private ExpenseAdapter expenseAdapter;
    private Uri selectedExpenseReceiptUri = null;

    // Format for dates
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    private CardView cardBudgetTracker;
    private ProgressBar progressBudget;
    private TextView tvBudgetPercentage, tvBudgetSpent, tvBudgetRemaining;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        // Get trip ID from arguments
        Bundle args = getArguments();
        if (args != null) {
            tripId = args.getString("trip_id");
        }

        if (tripId == null || tripId.isEmpty()) {
            Toast.makeText(requireContext(), "Trip ID is missing", Toast.LENGTH_SHORT).show();
            // Navigate back
            getParentFragmentManager().popBackStack();
            return;
        }

        // Initialize FirestoreManager
        firestoreManager = FirestoreManager.getInstance();

        // Initialize views
        initializeViews(view);

        // Initialize MapView
        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Load trip data
        loadTripData();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Configure map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // If trip is already loaded, display its locations on the map
        if (currentTrip != null && currentTrip.getLocations() != null) {
            displayLocationsOnMap(currentTrip.getLocations());
        }
    }

    private void initializeViews(View view) {
        ivTripImage = view.findViewById(R.id.iv_trip_image);
        tvTripName = view.findViewById(R.id.tv_trip_name);
        tvDestination = view.findViewById(R.id.tv_destination);
        tvDateRange = view.findViewById(R.id.tv_date_range);
        tvBudget = view.findViewById(R.id.tv_budget);
        tvFlightNumber = view.findViewById(R.id.tv_flight_number);
        tvNotes = view.findViewById(R.id.tv_notes);
        rvDayContainers = view.findViewById(R.id.rv_day_containers);
        rvDocuments = view.findViewById(R.id.rv_documents);
        btnEditTrip = view.findViewById(R.id.btn_edit_trip);
        btnDeleteTrip = view.findViewById(R.id.btn_delete_trip);
        emptyStateView = view.findViewById(R.id.empty_state_view);
        cardBudgetTracker = view.findViewById(R.id.card_budget_tracker);
        progressBudget = view.findViewById(R.id.progress_budget);
        tvBudgetPercentage = view.findViewById(R.id.tv_budget_percentage);
        tvBudgetSpent = view.findViewById(R.id.tv_budget_spent);
        tvBudgetRemaining = view.findViewById(R.id.tv_budget_remaining);

        // Set up RecyclerViews
        rvDayContainers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Set up button click listeners
        btnEditTrip.setOnClickListener(v -> openTripEditor());
        btnDeleteTrip.setOnClickListener(v -> confirmDeleteTrip());

        // Initialize expense section
        rvExpenses = view.findViewById(R.id.rv_expenses);
        rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        btnAddExpense = view.findViewById(R.id.btn_add_expense);

        expenseAdapter = new ExpenseAdapter(requireContext(), new ExpenseAdapter.ExpenseAdapterListener() {
            @Override
            public void onViewExpense(Expense expense, int position) {
                // Show expense details or receipt in a dialog
                showExpenseDetailsDialog(expense);
            }

            @Override
            public void onEditExpense(Expense expense, int position) {
                // Show edit dialog
                showAddEditExpenseDialog(expense, position);
            }

            @Override
            public void onDeleteExpense(Expense expense, int position) {
                // Confirm and delete
                confirmDeleteExpense(expense, position);
            }
        });

        rvExpenses.setAdapter(expenseAdapter);

        btnAddExpense.setOnClickListener(v -> {
            showAddEditExpenseDialog(null, -1);
        });
    }

    private void loadTripData() {
        firestoreManager.getTripById(tripId, new FirestoreManager.DataCallback<Trip>() {
            @Override
            public void onSuccess(Trip trip) {
                if (trip != null) {
                    currentTrip = trip;
                    displayTripDetails(trip);
                } else {
                    Toast.makeText(requireContext(), "Trip not found", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(),
                        "Error loading trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void displayTripDetails(Trip trip) {
        // Set trip details
        tvTripName.setText(trip.getName());
        tvDestination.setText(trip.getDestination());

        // Set date range
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            String dateRange = dateFormat.format(trip.getStartDate()) + " - "
                    + dateFormat.format(trip.getEndDate());
            tvDateRange.setText(dateRange);
        }

        // Set budget
        if (trip.getBudget() > 0) {
            tvBudget.setText(String.format(Locale.getDefault(),
                    "Budget: %.2f %s", trip.getBudget(), trip.getCurrency()));
            tvBudget.setVisibility(View.VISIBLE);
        } else {
            tvBudget.setVisibility(View.GONE);
        }

        // Set flight number
        if (trip.getFlightNumber() != null && !trip.getFlightNumber().isEmpty()) {
            tvFlightNumber.setText("Flight: " + trip.getFlightNumber());
            tvFlightNumber.setVisibility(View.VISIBLE);
        } else {
            tvFlightNumber.setVisibility(View.GONE);
        }

        // Set notes
        if (trip.getNotes() != null && !trip.getNotes().isEmpty()) {
            tvNotes.setText(trip.getNotes());
            tvNotes.setVisibility(View.VISIBLE);
        } else {
            tvNotes.setVisibility(View.GONE);
        }

        // Load image using Glide
        if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(trip.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(ivTripImage);
            ivTripImage.setVisibility(View.VISIBLE);
        } else {
            ivTripImage.setVisibility(View.GONE);
        }

        // Display locations by day
        displayLocations(trip);

        // Also display locations on the map
        if (googleMap != null && trip.getLocations() != null) {
            displayLocationsOnMap(trip.getLocations());
        }

        // Display documents
        displayDocuments(trip);

        // Display expenses
        displayExpenses(trip);

        updateBudgetTracker();
    }

    private void displayLocations(Trip trip) {
        if (trip.getLocations() != null && !trip.getLocations().isEmpty()) {
            // Group locations by day
            Map<Integer, List<Location>> locationsByDay = new HashMap<>();

            // Find the max day index to determine how many days to display
            int maxDayIndex = 1;

            for (Location location : trip.getLocations()) {
                int dayIndex = location.getDayIndex();
                if (dayIndex > maxDayIndex) {
                    maxDayIndex = dayIndex;
                }

                if (!locationsByDay.containsKey(dayIndex)) {
                    locationsByDay.put(dayIndex, new ArrayList<>());
                }
                locationsByDay.get(dayIndex).add(location);
            }

            // Create adapters for each day and add to a parent adapter
            List<DayItineraryItem> dayItems = new ArrayList<>();

            for (int i = 1; i <= maxDayIndex; i++) {
                List<Location> dayLocations = locationsByDay.getOrDefault(i, new ArrayList<>());
                if (!dayLocations.isEmpty()) {
                    // Calculate the date for this day based on trip start date and day index
                    Date dayDate = null;
                    if (trip.getStartDate() != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(trip.getStartDate());
                        cal.add(Calendar.DAY_OF_MONTH, i - 1); // i is 1-indexed
                        dayDate = cal.getTime();
                    }

                    DayItineraryItem dayItem = new DayItineraryItem("Day " + i, dayDate, dayLocations);
                    dayItems.add(dayItem);
                }
            }

            // Create and set adapter
            DayItineraryAdapter adapter = new DayItineraryAdapter(requireContext(), dayItems);
            rvDayContainers.setAdapter(adapter);
            rvDayContainers.setVisibility(View.VISIBLE);
        } else {
            // No locations to display
            rvDayContainers.setVisibility(View.GONE);

            // Optionally show an empty state message
            TextView emptyLocationsText = new TextView(requireContext());
            emptyLocationsText.setText("No itinerary locations added yet.");
            emptyLocationsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyLocationsText.setPadding(0, 16, 0, 16);

            // Add to a container or handle as appropriate for your layout
        }
    }

    private void displayDocuments(Trip trip) {
        if (trip.getDocuments() != null && !trip.getDocuments().isEmpty()) {
            DocumentAdapter adapter = new DocumentAdapter(requireContext(),
                    new DocumentAdapter.DocumentAdapterListener() {
                @Override
                public void onViewDocument(edu.cuhk.a3310_final_proj.models.Document document, int position) {
                    // Open document
                    // Implement viewing document logic
                }

                @Override
                public void onDeleteDocument(edu.cuhk.a3310_final_proj.models.Document document, int position) {
                    // This is a detail view, so maybe disable deletion here
                }
            });
            adapter.setDocuments(new ArrayList<>(trip.getDocuments()));
            rvDocuments.setAdapter(adapter);
            rvDocuments.setVisibility(View.VISIBLE);
        } else {
            rvDocuments.setVisibility(View.GONE);
        }
    }

    private void displayExpenses(Trip trip) {
        if (trip.getExpenses() != null && !trip.getExpenses().isEmpty()) {
            expenseAdapter.setExpenses(trip.getExpenses());
            rvExpenses.setVisibility(View.VISIBLE);
        } else {
            rvExpenses.setVisibility(View.GONE);
        }
    }

    private void openTripEditor() {
        Fragment tripPlanningFragment = new TripPlanningFragment();

        Bundle args = new Bundle();
        args.putString("trip_id", tripId);
        tripPlanningFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, tripPlanningFragment)
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteTrip() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete this trip?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTrip();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTrip() {
        firestoreManager.deleteTrip(tripId, new FirestoreManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(requireContext(), "Trip deleted successfully", Toast.LENGTH_SHORT).show();

                // Navigate back to trip list
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(),
                        "Failed to delete trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showExpenseDetailsDialog(Expense expense) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_expense_details, null);

        TextView tvAmount = dialogView.findViewById(R.id.tv_detail_amount);
        TextView tvCategory = dialogView.findViewById(R.id.tv_detail_category);
        TextView tvDate = dialogView.findViewById(R.id.tv_detail_date);
        TextView tvDescription = dialogView.findViewById(R.id.tv_detail_description);
        ImageView ivReceipt = dialogView.findViewById(R.id.iv_detail_receipt);

        // Set values
        tvAmount.setText(String.format(Locale.getDefault(), "%s %.2f",
                expense.getCurrency(), expense.getAmount()));
        tvCategory.setText(expense.getCategory());

        if (expense.getDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            tvDate.setText(dateFormat.format(expense.getDate()));
        }

        if (expense.getDescription() != null && !expense.getDescription().isEmpty()) {
            tvDescription.setText(expense.getDescription());
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        // Load receipt image if available
        if (expense.getReceiptImageUrl() != null && !expense.getReceiptImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(expense.getReceiptImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(ivReceipt);
            ivReceipt.setVisibility(View.VISIBLE);
        } else {
            ivReceipt.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Expense Details")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private void showAddEditExpenseDialog(Expense expenseToEdit, int position) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Initialize views
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_expense_amount);
        AutoCompleteTextView dropdownCurrency = dialogView.findViewById(R.id.dropdown_currency);
        TextInputEditText etCategory = dialogView.findViewById(R.id.et_expense_category);
        TextInputEditText etDescription = dialogView.findViewById(R.id.et_expense_description);
        TextInputEditText etDate = dialogView.findViewById(R.id.et_expense_date);
        ImageView ivReceiptPreview = dialogView.findViewById(R.id.iv_receipt_preview);
        Button btnAddReceipt = dialogView.findViewById(R.id.btn_add_receipt);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_save);

        // Set up currency dropdown
        String[] currencies = getResources().getStringArray(R.array.currencies);
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                currencies
        );
        dropdownCurrency.setAdapter(currencyAdapter);

        // Set up date picker
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        etDate.setText(dateFormat.format(new Date()));
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            if (expenseToEdit != null && expenseToEdit.getDate() != null) {
                calendar.setTime(expenseToEdit.getDate());
            }

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        etDate.setText(dateFormat.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });

        // If editing, populate with existing data
        if (expenseToEdit != null) {
            tvTitle.setText("Edit Expense");
            etAmount.setText(String.valueOf(expenseToEdit.getAmount()));
            dropdownCurrency.setText(expenseToEdit.getCurrency(), false);
            etCategory.setText(expenseToEdit.getCategory());
            etDescription.setText(expenseToEdit.getDescription());

            if (expenseToEdit.getDate() != null) {
                etDate.setText(dateFormat.format(expenseToEdit.getDate()));
            }

            // Load receipt image
            if (expenseToEdit.getReceiptImageUrl() != null && !expenseToEdit.getReceiptImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(expenseToEdit.getReceiptImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(ivReceiptPreview);
                ivReceiptPreview.setVisibility(View.VISIBLE);
            }
        }

        // Set up receipt image selection
        btnAddReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Receipt Image"), PICK_EXPENSE_RECEIPT_REQUEST);
        });

        // Handle button clicks
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            // Validate inputs
            if (etAmount.getText().toString().isEmpty()) {
                etAmount.setError("Amount is required");
                return;
            }

            if (etCategory.getText().toString().isEmpty()) {
                etCategory.setError("Category is required");
                return;
            }

            // Create or update expense
            Expense expense = (expenseToEdit != null) ? expenseToEdit : new Expense();
            expense.setAmount(Double.parseDouble(etAmount.getText().toString()));
            expense.setCurrency(dropdownCurrency.getText().toString());
            expense.setCategory(etCategory.getText().toString());
            expense.setDescription(etDescription.getText().toString());

            try {
                Date date = dateFormat.parse(etDate.getText().toString());
                expense.setDate(date);
            } catch (ParseException e) {
                expense.setDate(new Date());
            }

            if (selectedExpenseReceiptUri != null) {
                // Upload receipt image and save expense
                uploadExpenseReceiptAndSave(expense, selectedExpenseReceiptUri, position);
                selectedExpenseReceiptUri = null;
            } else {
                // Save expense without image
                saveExpense(expense, position);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void confirmDeleteExpense(Expense expense, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Remove expense from list
                    currentTrip.getExpenses().remove(position);
                    expenseAdapter.removeExpense(position);

                    // Update trip in Firestore
                    updateTripInFirestore();

                    // Add this line to update the budget tracker immediately
                    updateBudgetTracker();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadExpenseReceiptAndSave(Expense expense, Uri imageUri, int position) {
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Uploading receipt...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create unique file name for receipt
        String fileName = "receipt_" + System.currentTimeMillis() + ".jpg";
        StorageReference receiptRef = FirebaseStorage.getInstance().getReference()
                .child("receipts")
                .child(currentTrip.getId())
                .child(fileName);

        receiptRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    receiptRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Set receipt URL to expense
                        expense.setReceiptImageUrl(uri.toString());

                        // Save expense
                        saveExpense(expense, position);

                        progressDialog.dismiss();
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Failed to upload receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveExpense(Expense expense, int position) {
        // Set trip ID and generate ID if new
        expense.setTripId(currentTrip.getId());
        if (expense.getId() == null || expense.getId().isEmpty()) {
            expense.setId(UUID.randomUUID().toString());
        }

        // Add to or update in trip's expense list
        if (position == -1) {
            // New expense
            if (currentTrip.getExpenses() == null) {
                currentTrip.setExpenses(new ArrayList<>());
            }
            currentTrip.getExpenses().add(expense);
            expenseAdapter.addExpense(expense);
        } else {
            // Update existing expense
            currentTrip.getExpenses().set(position, expense);
            expenseAdapter.updateExpense(expense, position);
        }

        // Update trip in Firestore
        updateTripInFirestore();

        // Update budget tracker immediately for better UX
        updateBudgetTracker();
    }

    private void updateTripInFirestore() {
        // Add this debugging
        if (currentTrip.getExpenses() != null) {
            Log.d("TripDetailFragment", "Saving trip with " + currentTrip.getExpenses().size() + " expenses");
            for (Expense expense : currentTrip.getExpenses()) {
                Log.d("TripDetailFragment", "Expense: " + expense.getAmount() + " " + expense.getCurrency() + " - " + expense.getCategory());
            }
        } else {
            Log.d("TripDetailFragment", "Expenses list is null!");
        }

        firestoreManager.saveTrip(currentTrip, new FirestoreManager.DataCallback<Trip>() {
            @Override
            public void onSuccess(Trip result) {
                Toast.makeText(requireContext(), "Trip updated successfully", Toast.LENGTH_SHORT).show();

                // Add this line to ensure the UI is refreshed
                displayExpenses(currentTrip);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to update trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("TripDetailFragment", "Failed to update trip", e);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_EXPENSE_RECEIPT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedExpenseReceiptUri = data.getData();

            // Update preview in dialog if it's visible
            // This approach won't work because we're in a Fragment, not a DialogFragment
            // Instead, store the URI and let the next dialog access use it
            // The dialog will have been dismissed by this point anyway
        }
    }

    private void updateBudgetTracker() {
        if (currentTrip == null || currentTrip.getBudget() <= 0) {
            // Hide budget tracker if there's no budget set
            cardBudgetTracker.setVisibility(View.GONE);
            return;
        }

        cardBudgetTracker.setVisibility(View.VISIBLE);

        // Calculate total expenses
        double totalExpenses = 0;
        if (currentTrip.getExpenses() != null && !currentTrip.getExpenses().isEmpty()) {
            // For simplicity, we'll assume all expenses are in the same currency as the budget
            // In a real app, you'd want to handle currency conversion
            for (Expense expense : currentTrip.getExpenses()) {
                totalExpenses += expense.getAmount();
            }
        }

        // Calculate budget metrics
        double budgetAmount = currentTrip.getBudget();
        double remainingBudget = budgetAmount - totalExpenses;
        int percentageUsed = (int) Math.min(100, (totalExpenses / budgetAmount) * 100);

        // Update UI elements
        progressBudget.setProgress(percentageUsed);
        tvBudgetPercentage.setText(percentageUsed + "%");

        String currency = currentTrip.getCurrency();
        tvBudgetSpent.setText(String.format(Locale.getDefault(),
                "Spent: %s %.2f", currency, totalExpenses));
        tvBudgetRemaining.setText(String.format(Locale.getDefault(),
                "Remaining: %s %.2f", currency, remainingBudget));

        // Set progress bar color based on percentage used
        int colorId;
        if (percentageUsed < 70) {
            colorId = R.color.budget_good; // Define this color in your colors.xml (e.g., green)
        } else if (percentageUsed < 90) {
            colorId = R.color.budget_warning; // Define this color (e.g., yellow/orange)
        } else {
            colorId = R.color.budget_danger; // Define this color (e.g., red)
        }

        // Apply the color to the progress bar
        progressBudget.getProgressDrawable().setColorFilter(
                ContextCompat.getColor(requireContext(), colorId),
                PorterDuff.Mode.SRC_IN);
    }

    private void displayLocationsOnMap(List<Location> locations) {
        if (googleMap == null || locations == null || locations.isEmpty()) {
            return;
        }

        // Clear existing markers
        for (Marker marker : locationMarkers) {
            marker.remove();
        }
        locationMarkers.clear();

        // Create bounds to include all locations
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidCoordinates = false;

        // Add markers for each location
        for (Location location : locations) {
            // Skip locations without coordinates
            if (location.getLatitude() == 0 && location.getLongitude() == 0) {
                continue;
            }

            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            boundsBuilder.include(position);
            hasValidCoordinates = true;

            // Add marker with info
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(location.getName()));

            if (marker != null) {
                locationMarkers.add(marker);
            }
        }

        // If we have valid locations, zoom to fit them all
        if (hasValidCoordinates) {
            try {
                int padding = 100; // Padding around markers in pixels
                LatLngBounds bounds = boundsBuilder.build();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {
                // Fallback to center on first location
                if (!locationMarkers.isEmpty()) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            locationMarkers.get(0).getPosition(), 12f));
                }
            }
        } else {
            // If no valid coordinates, show a default view (e.g., destination city)
            // You'd need to geocode the trip's destination to get coordinates
            geocodeDestination(currentTrip.getDestination());
        }
    }

    private void geocodeDestination(String destination) {
        // Use Geocoder to get coordinates for the destination
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(destination, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng position = new LatLng(address.getLatitude(), address.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10f));
            }
        } catch (IOException e) {
            Log.e("TripDetailFragment", "Error geocoding destination", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
