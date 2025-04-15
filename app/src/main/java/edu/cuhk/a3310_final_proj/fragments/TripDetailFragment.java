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
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.provider.CalendarContract;
import android.content.pm.PackageManager;
import android.database.Cursor;

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
import java.util.TimeZone;
import java.util.UUID;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.DayItineraryAdapter;
import edu.cuhk.a3310_final_proj.adapters.DocumentAdapter;
import edu.cuhk.a3310_final_proj.adapters.ExpenseAdapter;
import edu.cuhk.a3310_final_proj.models.DayItineraryItem;
import edu.cuhk.a3310_final_proj.models.Location;
import edu.cuhk.a3310_final_proj.firebase.FirestoreManager;
import edu.cuhk.a3310_final_proj.models.Expense;
import edu.cuhk.a3310_final_proj.models.Trip;

public class TripDetailFragment extends Fragment implements OnMapReadyCallback {

    private static final int PICK_EXPENSE_RECEIPT_REQUEST = 3;

    private DocumentAdapter documentAdapter;
    private MapView mapView;
    private GoogleMap googleMap;
    private List<Marker> locationMarkers = new ArrayList<>();

    private String tripId;
    private FirestoreManager firestoreManager;
    private Trip currentTrip;
    private static final int REQUEST_CALENDAR_PERMISSION = 100;
    private String trip_id;

    @PropertyName("trip_id")
    public String getTripId() {
        return trip_id;
    }

    @PropertyName("trip_id")
    public void setTripId(String tripId) {
        this.trip_id = tripId;
    }

    private ImageView ivTripImage;
    private TextView tvTripName, tvDestination, tvDateRange, tvBudget, tvFlightNumber, tvNotes;
    private RecyclerView rvDayContainers, rvDocuments, rvExpenses;
    private Button btnEditTrip, btnDeleteTrip, btnAddExpense;
    private View emptyStateView;

    private ExpenseAdapter expenseAdapter;
    private Uri selectedExpenseReceiptUri = null;

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
            getParentFragmentManager().popBackStack();
            return;
        }

        firestoreManager = FirestoreManager.getInstance();

        initializeViews(view);

        mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        loadTripData();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);

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
        Button btnAddToCalendar = view.findViewById(R.id.btn_add_to_calendar);
        btnAddToCalendar.setOnClickListener(v -> addTripToCalendar());
        rvDayContainers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnEditTrip.setOnClickListener(v -> openTripEditor());
        btnDeleteTrip.setOnClickListener(v -> confirmDeleteTrip());

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

        tvTripName.setText(trip.getName());
        tvDestination.setText(trip.getDestination());

        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            String dateRange = dateFormat.format(trip.getStartDate()) + " - "
                    + dateFormat.format(trip.getEndDate());
            tvDateRange.setText(dateRange);
        }

        if (trip.getBudget() > 0) {
            tvBudget.setText(String.format(Locale.getDefault(),
                    "Budget: %.2f %s", trip.getBudget(), trip.getCurrency()));
            tvBudget.setVisibility(View.VISIBLE);
        } else {
            tvBudget.setVisibility(View.GONE);
        }

        if (trip.getFlightNumber() != null && !trip.getFlightNumber().isEmpty()) {
            tvFlightNumber.setText("Flight: " + trip.getFlightNumber());
            tvFlightNumber.setVisibility(View.VISIBLE);
        } else {
            tvFlightNumber.setVisibility(View.GONE);
        }

        if (trip.getNotes() != null && !trip.getNotes().isEmpty()) {
            tvNotes.setText(trip.getNotes());
            tvNotes.setVisibility(View.VISIBLE);
        } else {
            tvNotes.setVisibility(View.GONE);
        }

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

        displayLocations(trip);

        if (googleMap != null && trip.getLocations() != null) {
            displayLocationsOnMap(trip.getLocations());
        }

        displayDocuments(trip);

        displayExpenses(trip);

        updateBudgetTracker();
    }

    private void displayLocations(Trip trip) {
        if (trip.getLocations() != null && !trip.getLocations().isEmpty()) {

            Map<Integer, List<Location>> locationsByDay = new HashMap<>();

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

            List<DayItineraryItem> dayItems = new ArrayList<>();

            for (int i = 1; i <= maxDayIndex; i++) {
                List<Location> dayLocations = locationsByDay.getOrDefault(i, new ArrayList<>());
                if (!dayLocations.isEmpty()) {

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

            DayItineraryAdapter adapter = new DayItineraryAdapter(requireContext(), dayItems);
            rvDayContainers.setAdapter(adapter);
            rvDayContainers.setVisibility(View.VISIBLE);
        } else {

            rvDayContainers.setVisibility(View.GONE);

            TextView emptyLocationsText = new TextView(requireContext());
            emptyLocationsText.setText("No itinerary locations added yet.");
            emptyLocationsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyLocationsText.setPadding(0, 16, 0, 16);

        }
    }

    private void displayDocuments(Trip trip) {
        if (trip.getDocuments() != null && !trip.getDocuments().isEmpty()) {
            documentAdapter = new DocumentAdapter(requireContext(),
                    new DocumentAdapter.DocumentAdapterListener() {
                @Override
                public void onViewDocument(edu.cuhk.a3310_final_proj.models.Document document, int position) {
                    if (document.getFileUrl() != null && !document.getFileUrl().isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(document.getFileUrl()));
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException e) {
                            Toast.makeText(requireContext(),
                                    "No app found to open this document type",
                                    Toast.LENGTH_SHORT).show();
                            Log.e("TripDetailFragment", "No app to open document: " + e.getMessage());
                        } catch (Exception e) {
                            Toast.makeText(requireContext(),
                                    "Error opening document: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            Log.e("TripDetailFragment", "Error opening document", e);
                        }
                    } else {
                        Toast.makeText(requireContext(),
                                "Document URL is missing or invalid",
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDeleteDocument(edu.cuhk.a3310_final_proj.models.Document document, int position) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Document")
                            .setMessage("Are you sure you want to delete " + document.getName() + "?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                // Remove document from trip
                                if (currentTrip.getDocuments() != null) {
                                    currentTrip.getDocuments().remove(position);

                                    documentAdapter.notifyItemRemoved(position);

                                    if (currentTrip.getDocuments().isEmpty()) {
                                        rvDocuments.setVisibility(View.GONE);
                                    }

                                    updateTripInFirestore();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
            documentAdapter.setDocuments(new ArrayList<>(trip.getDocuments()));
            rvDocuments.setAdapter(documentAdapter);
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

        String[] currencies = getResources().getStringArray(R.array.currencies);
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                currencies
        );
        dropdownCurrency.setAdapter(currencyAdapter);

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

        if (expenseToEdit != null) {
            tvTitle.setText("Edit Expense");
            etAmount.setText(String.valueOf(expenseToEdit.getAmount()));
            dropdownCurrency.setText(expenseToEdit.getCurrency(), false);
            etCategory.setText(expenseToEdit.getCategory());
            etDescription.setText(expenseToEdit.getDescription());

            if (expenseToEdit.getDate() != null) {
                etDate.setText(dateFormat.format(expenseToEdit.getDate()));
            }

            if (expenseToEdit.getReceiptImageUrl() != null && !expenseToEdit.getReceiptImageUrl().isEmpty()) {
                Glide.with(requireContext())
                        .load(expenseToEdit.getReceiptImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .into(ivReceiptPreview);
                ivReceiptPreview.setVisibility(View.VISIBLE);
            }
        }

        btnAddReceipt.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Receipt Image"), PICK_EXPENSE_RECEIPT_REQUEST);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {

            if (etAmount.getText().toString().isEmpty()) {
                etAmount.setError("Amount is required");
                return;
            }

            if (etCategory.getText().toString().isEmpty()) {
                etCategory.setError("Category is required");
                return;
            }

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

                uploadExpenseReceiptAndSave(expense, selectedExpenseReceiptUri, position);
                selectedExpenseReceiptUri = null;
            } else {
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
                    currentTrip.getExpenses().remove(position);
                    expenseAdapter.removeExpense(position);

                    updateTripInFirestore();

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

        String fileName = "receipt_" + System.currentTimeMillis() + ".jpg";
        StorageReference receiptRef = FirebaseStorage.getInstance().getReference()
                .child("receipts")
                .child(currentTrip.getId())
                .child(fileName);

        receiptRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    receiptRef.getDownloadUrl().addOnSuccessListener(uri -> {

                        expense.setReceiptImageUrl(uri.toString());

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

        expense.setTripId(currentTrip.getId());
        if (expense.getId() == null || expense.getId().isEmpty()) {
            expense.setId(UUID.randomUUID().toString());
        }

        if (position == -1) {

            if (currentTrip.getExpenses() == null) {
                currentTrip.setExpenses(new ArrayList<>());
            }
            currentTrip.getExpenses().add(expense);
            expenseAdapter.addExpense(expense);
        } else {

            currentTrip.getExpenses().set(position, expense);
            expenseAdapter.updateExpense(expense, position);
        }

        updateTripInFirestore();

        updateBudgetTracker();
    }

    private void updateTripInFirestore() {

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

        }
    }

    private void updateBudgetTracker() {
        if (currentTrip == null || currentTrip.getBudget() <= 0) {

            cardBudgetTracker.setVisibility(View.GONE);
            return;
        }

        cardBudgetTracker.setVisibility(View.VISIBLE);

        double totalExpenses = 0;
        if (currentTrip.getExpenses() != null && !currentTrip.getExpenses().isEmpty()) {

            for (Expense expense : currentTrip.getExpenses()) {
                totalExpenses += expense.getAmount();
            }
        }

        double budgetAmount = currentTrip.getBudget();
        double remainingBudget = budgetAmount - totalExpenses;
        int percentageUsed = (int) Math.min(100, (totalExpenses / budgetAmount) * 100);

        progressBudget.setProgress(percentageUsed);
        tvBudgetPercentage.setText(percentageUsed + "%");

        String currency = currentTrip.getCurrency();
        tvBudgetSpent.setText(String.format(Locale.getDefault(),
                "Spent: %s %.2f", currency, totalExpenses));
        tvBudgetRemaining.setText(String.format(Locale.getDefault(),
                "Remaining: %s %.2f", currency, remainingBudget));

        int colorId;
        if (percentageUsed < 70) {
            colorId = R.color.budget_good;
        } else if (percentageUsed < 90) {
            colorId = R.color.budget_warning;
        } else {
            colorId = R.color.budget_danger;
        }

        progressBudget.getProgressDrawable().setColorFilter(
                ContextCompat.getColor(requireContext(), colorId),
                PorterDuff.Mode.SRC_IN);
    }

    private void displayLocationsOnMap(List<Location> locations) {
        if (googleMap == null || locations == null || locations.isEmpty()) {
            return;
        }

        for (Marker marker : locationMarkers) {
            marker.remove();
        }
        locationMarkers.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidCoordinates = false;

        for (Location location : locations) {

            if (location.getLatitude() == 0 && location.getLongitude() == 0) {
                continue;
            }

            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            boundsBuilder.include(position);
            hasValidCoordinates = true;

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
                int padding = 100;
                LatLngBounds bounds = boundsBuilder.build();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            } catch (Exception e) {

                if (!locationMarkers.isEmpty()) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            locationMarkers.get(0).getPosition(), 12f));
                }
            }
        } else {

            geocodeDestination(currentTrip.getDestination());
        }
    }

    private void geocodeDestination(String destination) {

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

    private void addTripToCalendar() {
        // Check for both calendar permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            }, REQUEST_CALENDAR_PERMISSION);
            return;
        }

        if (currentTrip != null && currentTrip.getStartDate() != null && currentTrip.getEndDate() != null) {
            try {
                ContentResolver cr = requireActivity().getContentResolver();
                ContentValues values = new ContentValues();

                // Event details
                values.put(CalendarContract.Events.TITLE, currentTrip.getName());
                values.put(CalendarContract.Events.DESCRIPTION,
                        "Trip to " + currentTrip.getDestination());
                values.put(CalendarContract.Events.EVENT_LOCATION, currentTrip.getDestination());

                // Create a UTC calendar instance for all-day events
                TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
                Calendar startCalendar = Calendar.getInstance(utcTimeZone);
                startCalendar.setTime(currentTrip.getStartDate());
                // Set time to midnight
                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                startCalendar.set(Calendar.MILLISECOND, 0);

                // Add one day to compensate for timezone issues
                startCalendar.add(Calendar.DAY_OF_MONTH, 1);

                Calendar endCalendar = Calendar.getInstance(utcTimeZone);
                endCalendar.setTime(currentTrip.getEndDate());
                // Set time to midnight
                endCalendar.set(Calendar.HOUR_OF_DAY, 0);
                endCalendar.set(Calendar.MINUTE, 0);
                endCalendar.set(Calendar.SECOND, 0);
                endCalendar.set(Calendar.MILLISECOND, 0);
                // Add two days - one for inclusive end date, one for timezone
                endCalendar.add(Calendar.DAY_OF_MONTH, 2);

                // Convert to milliseconds
                long startMillis = startCalendar.getTimeInMillis();
                long endMillis = endCalendar.getTimeInMillis();

                // Set the event values for an all-day event
                values.put(CalendarContract.Events.DTSTART, startMillis);
                values.put(CalendarContract.Events.DTEND, endMillis);
                values.put(CalendarContract.Events.ALL_DAY, 1);
                values.put(CalendarContract.Events.EVENT_TIMEZONE, "UTC");

                // Get default calendar ID
                Cursor cursor = cr.query(
                        CalendarContract.Calendars.CONTENT_URI,
                        new String[]{CalendarContract.Calendars._ID},
                        null, null, null);

                long calendarId = 1; // Default fallback
                if (cursor != null && cursor.moveToFirst()) {
                    calendarId = cursor.getLong(0);
                    cursor.close();
                }

                values.put(CalendarContract.Events.CALENDAR_ID, calendarId);

                // Add event
                Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

                Toast.makeText(requireContext(), "Trip added to calendar", Toast.LENGTH_SHORT).show();

                // Log the dates for debugging
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Log.d("TripDetailFragment", "Original start date: " + sdf.format(currentTrip.getStartDate()));
                Log.d("TripDetailFragment", "Original end date: " + sdf.format(currentTrip.getEndDate()));
                Log.d("TripDetailFragment", "Calendar start: " + sdf.format(new Date(startMillis)));
                Log.d("TripDetailFragment", "Calendar end: " + sdf.format(new Date(endMillis)));

            } catch (Exception e) {
                Log.e("TripDetailFragment", "Error adding trip to calendar", e);
                Toast.makeText(requireContext(), "Failed to add trip to calendar", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Trip dates not set", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CALENDAR_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, add to calendar
                addTripToCalendar();
            } else {
                Toast.makeText(requireContext(), "Calendar permission required to add trip", Toast.LENGTH_SHORT).show();
            }
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
