package edu.cuhk.a3310_final_proj.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.PropertyName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.DocumentAdapter;
import edu.cuhk.a3310_final_proj.adapters.ExpenseAdapter;
import edu.cuhk.firebase.FirestoreManager;
import edu.cuhk.a3310_final_proj.models.Expense;
import edu.cuhk.a3310_final_proj.models.Trip;

public class TripDetailFragment extends Fragment {

    private static final int PICK_EXPENSE_RECEIPT_REQUEST = 3;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // Load trip data
        loadTripData();
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

        // Display documents
        displayDocuments(trip);

        // Display expenses
        displayExpenses(trip);
    }

    private void displayLocations(Trip trip) {
        // Implementation depends on your UI design for displaying locations
        // This is just a placeholder
        if (trip.getLocations() != null && !trip.getLocations().isEmpty()) {
            // Create a day-by-day view of locations
            // This would typically use a custom adapter
        } else {
            // Show empty state for locations
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
    }

    private void updateTripInFirestore() {
        firestoreManager.saveTrip(currentTrip, new FirestoreManager.DataCallback<Trip>() {
            @Override
            public void onSuccess(Trip result) {
                Toast.makeText(requireContext(), "Trip updated successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to update trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
}
