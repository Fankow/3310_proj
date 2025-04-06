package edu.cuhk.a3310_final_proj.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

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
import edu.cuhk.a3310_final_proj.adapters.DocumentAdapter;
import edu.cuhk.a3310_final_proj.adapters.LocationAdapter;
import edu.cuhk.a3310_final_proj.adapters.PlacesAutocompleteAdapter;
import edu.cuhk.a3310_final_proj.firebase.FirestoreManager;
import edu.cuhk.a3310_final_proj.firebase.StorageManager;
import edu.cuhk.a3310_final_proj.models.Document;
import edu.cuhk.a3310_final_proj.models.Location;
import edu.cuhk.a3310_final_proj.models.Trip;

public class TripPlanningFragment extends Fragment implements PlacesAutocompleteAdapter.PlaceAutocompleteListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_DOCUMENT_REQUEST = 2;
    private static final String TAG = "TripPlanningFragment";

    // UI components
    private TextInputEditText etTripName, etStartDate, etEndDate,
            etFlightNumber, etBudget, etNotes;
    private AutoCompleteTextView etDestination, dropdownCurrency;
    private Button btnSelectImage, btnSaveTrip, btnAddDocument;
    private ImageView tripImage;
    private LinearLayout daysContainer;
    private Button btnAddDay;
    private RecyclerView rvDocuments;

    // Managers
    private FirestoreManager firestoreManager;
    private StorageManager storageManager;

    // Data
    private Uri selectedImageUri = null;
    private Uri selectedDocumentUri;
    private String selectedDocumentName;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
    private SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private Trip currentTrip = null;
    private String tripId = null; // Will be set if editing an existing trip
    private PlacesAutocompleteAdapter placesAdapter;
    private PlacesAutocompleteAdapter destinationAdapter;
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private Map<Integer, List<Location>> locationsByDay = new HashMap<>();
    private Map<Integer, RecyclerView> dayRecyclerViews = new HashMap<>();
    private Map<Integer, LocationAdapter> locationAdapters = new HashMap<>();
    private int dayCount = 1;
    private DocumentAdapter documentAdapter;
    private List<Document> documentList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_planning, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get trip ID if passed (for editing an existing trip)
        Bundle args = getArguments();
        if (args != null) {
            tripId = args.getString("trip_id");
        }

        // Initialize managers
        firestoreManager = FirestoreManager.getInstance();
        storageManager = StorageManager.getInstance();

        // Initialize UI components
        initializeViews(view);
        setupDatePickers();
        setupCurrencyDropdown();
        setupImageSelection();
        setupSaveButton();
        setupLocationManagement();
        setupDocumentSection(view);
        setupPlaceAutocomplete();
        // Load existing trip data if editing
        if (tripId != null) {
            loadTripData(tripId);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Clean up Places resources
        if (destinationAdapter != null) {
            destinationAdapter.shutdown();
            destinationAdapter = null;
        }
    }

    private void initializeViews(View view) {
        etTripName = view.findViewById(R.id.et_trip_name);

        // Change the type to AutoCompleteTextView
        etDestination = (AutoCompleteTextView) view.findViewById(R.id.et_destination);

        etStartDate = view.findViewById(R.id.et_start_date);
        etEndDate = view.findViewById(R.id.et_end_date);
        etFlightNumber = view.findViewById(R.id.et_flight_number);
        etBudget = view.findViewById(R.id.et_budget);
        etNotes = view.findViewById(R.id.et_notes);
        dropdownCurrency = view.findViewById(R.id.dropdown_currency);
        btnSelectImage = view.findViewById(R.id.btn_select_image);
        btnSaveTrip = view.findViewById(R.id.btn_save_trip);
        tripImage = view.findViewById(R.id.trip_image);
        daysContainer = view.findViewById(R.id.days_container);
        btnAddDay = view.findViewById(R.id.btn_add_day);
    }

    private void setupDatePickers() {
        // Start date picker
        etStartDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year1, monthOfYear, dayOfMonth);
                        etStartDate.setText(dateFormat.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });

        // End date picker
        etEndDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year1, monthOfYear, dayOfMonth);
                        etEndDate.setText(dateFormat.format(selectedDate.getTime()));
                    }, year, month, day);
            datePickerDialog.show();
        });
    }

    private void setupCurrencyDropdown() {
        String[] currencies = {"USD", "EUR", "JPY", "GBP", "AUD", "CAD", "CHF", "CNY", "HKD"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                currencies
        );
        dropdownCurrency.setAdapter(adapter);
        dropdownCurrency.setText("USD", false);
    }

    private void setupImageSelection() {
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
        });
    }

    private void setupDocumentSection(View view) {
        rvDocuments = view.findViewById(R.id.rv_documents);
        rvDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));

        documentAdapter = new DocumentAdapter(requireContext(), new DocumentAdapter.DocumentAdapterListener() {
            @Override
            public void onViewDocument(Document document, int position) {
                // Open the document
                if (document.getFileUrl() != null && !document.getFileUrl().isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(document.getFileUrl()));
                    startActivity(intent);
                }
            }

            @Override
            public void onDeleteDocument(Document document, int position) {
                // Confirm and delete document
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Document")
                        .setMessage("Are you sure you want to delete " + document.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            documentAdapter.removeDocument(position);
                            documentList.remove(position);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        rvDocuments.setAdapter(documentAdapter);

        btnAddDocument = view.findViewById(R.id.btn_add_document);
        btnAddDocument.setOnClickListener(v -> {
            selectDocument();
        });

        // Initialize document fields
        TextInputEditText etDocumentName = view.findViewById(R.id.et_document_name);

        // If editing a trip, populate documents
        if (currentTrip != null && currentTrip.getDocuments() != null) {
            documentList.addAll(currentTrip.getDocuments());
            documentAdapter.setDocuments(documentList);
            if (!documentList.isEmpty()) {
                rvDocuments.setVisibility(View.VISIBLE);
            }
        }
    }

    private void selectDocument() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Document"), PICK_DOCUMENT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == -1 && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            tripImage.setImageURI(selectedImageUri);
            tripImage.setVisibility(View.VISIBLE);
        }

        if (requestCode == PICK_DOCUMENT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedDocumentUri = data.getData();

            // Get document name
            String fileName = "Document";
            Cursor cursor = requireContext().getContentResolver().query(selectedDocumentUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }

            selectedDocumentName = fileName;
            TextInputEditText etDocumentName = getView().findViewById(R.id.et_document_name);
            etDocumentName.setText(selectedDocumentName);

            // Upload document
            uploadDocumentToFirebase(selectedDocumentUri, selectedDocumentName);
        }
    }

    private void uploadDocumentToFirebase(Uri documentUri, String fileName) {
        // Show progress
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle("Uploading Document");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to upload documents", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        // Create unique filename
        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
        final String documentPath = "documents/" + currentUser.getUid() + "/" + uniqueFileName;

        // Get Firebase Storage reference
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference documentRef = storageRef.child(documentPath);

        // Upload file
        documentRef.putFile(documentUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    documentRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Create Document object
                        Document document = new Document();
                        document.setId(UUID.randomUUID().toString());
                        document.setName(fileName);
                        document.setFileUrl(uri.toString());

                        // Add to adapter and list
                        documentList.add(document);
                        documentAdapter.addDocument(document);
                        rvDocuments.setVisibility(View.VISIBLE);

                        // Clear fields
                        TextInputEditText etDocumentName = getView().findViewById(R.id.et_document_name);
                        etDocumentName.setText("");
                        selectedDocumentUri = null;
                        selectedDocumentName = null;

                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Document uploaded successfully", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    // Add more detailed error logging
                    Log.e("TripPlanningFragment", "Document upload failed", e);
                    if (e instanceof StorageException) {
                        StorageException storageException = (StorageException) e;
                        Log.e("TripPlanningFragment", "StorageException code: " + storageException.getErrorCode());
                    }

                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Failed to upload document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    progressDialog.setMessage("Uploading: " + (int) progress + "%");
                });
    }

    private void setupSaveButton() {
        btnSaveTrip.setOnClickListener(v -> {
            if (validateForm()) {
                saveTrip();
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        // Validate Trip Name
        if (etTripName.getText().toString().trim().isEmpty()) {
            etTripName.setError("Trip name is required");
            valid = false;
        }

        // Validate Destination
        if (etDestination.getText().toString().trim().isEmpty()) {
            etDestination.setError("Destination is required");
            valid = false;
        }

        // Validate Dates
        if (etStartDate.getText().toString().trim().isEmpty()) {
            etStartDate.setError("Start date is required");
            valid = false;
        }

        if (etEndDate.getText().toString().trim().isEmpty()) {
            etEndDate.setError("End date is required");
            valid = false;
        }

        // Validate date order (start date must be before or equal to end date)
        if (!etStartDate.getText().toString().trim().isEmpty()
                && !etEndDate.getText().toString().trim().isEmpty()) {
            try {
                Date startDate = dateFormat.parse(etStartDate.getText().toString());
                Date endDate = dateFormat.parse(etEndDate.getText().toString());

                if (startDate != null && endDate != null && startDate.after(endDate)) {
                    etEndDate.setError("End date must be after start date");
                    valid = false;
                }
            } catch (ParseException e) {
                e.printStackTrace();
                valid = false;
            }
        }

        // Validate Budget (if provided)
        if (!etBudget.getText().toString().trim().isEmpty()) {
            try {
                double budget = Double.parseDouble(etBudget.getText().toString());
                if (budget <= 0) {
                    etBudget.setError("Budget must be greater than 0");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                etBudget.setError("Invalid budget amount");
                valid = false;
            }
        }

        return valid;
    }

    private void saveTrip() {
        // Show progress
        btnSaveTrip.setEnabled(false);
        btnSaveTrip.setText("Saving...");

        // Create or update Trip object
        Trip trip = (currentTrip != null) ? currentTrip : new Trip();
        trip.setName(etTripName.getText().toString().trim());
        trip.setDestination(etDestination.getText().toString().trim());

        // Save all locations from all days
        List<Location> allLocations = new ArrayList<>();
        for (int i = 1; i <= dayCount; i++) {
            if (locationsByDay.containsKey(i)) {
                List<Location> dayLocations = locationsByDay.get(i);
                for (Location location : dayLocations) {
                    // Set which day this location belongs to
                    location.setDayIndex(i);
                    allLocations.add(location);
                }
            }
        }

        trip.setLocations(allLocations);

        // Save documents
        trip.setDocuments(documentList);

        // Parse dates
        try {
            if (etStartDate.getText() != null && etStartDate.getText().length() > 0) {
                trip.setStartDate(dateFormat.parse(etStartDate.getText().toString()));
            }
            if (!etEndDate.getText().isEmpty()) {
                trip.setEndDate(dateFormat.parse(etEndDate.getText().toString()));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        trip.setFlightNumber(etFlightNumber.getText().toString().trim());

        // Parse budget
        if (!etBudget.getText().toString().isEmpty()) {
            try {
                trip.setBudget(Double.parseDouble(etBudget.getText().toString()));
            } catch (NumberFormatException e) {
                trip.setBudget(0);
            }
        }

        trip.setCurrency(dropdownCurrency.getText().toString());
        trip.setNotes(etNotes.getText().toString().trim());

        // If we have a selected image, upload it first
        if (selectedImageUri != null) {
            uploadImageAndSaveTrip(trip);
        } else {
            // No image to upload, just save the trip
            saveTripToFirestore(trip);
        }
    }

    private void uploadImageAndSaveTrip(Trip trip) {
        String tripId = trip.getId() == null || trip.getId().isEmpty() ? "temp_" + System.currentTimeMillis() : trip.getId();

        StorageManager.getInstance().uploadTripImage(tripId, selectedImageUri, new StorageManager.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                trip.setImageUrl(downloadUrl);
                saveTripToFirestore(trip);
            }

            @Override
            public void onFailure(Exception e) {
                btnSaveTrip.setEnabled(true);
                btnSaveTrip.setText("Save Trip");
                Toast.makeText(requireContext(), "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTripToFirestore(Trip trip) {
        firestoreManager.saveTrip(trip, new FirestoreManager.DataCallback<Trip>() {
            @Override
            public void onSuccess(Trip result) {
                btnSaveTrip.setEnabled(true);
                btnSaveTrip.setText("Save Trip");
                Toast.makeText(requireContext(), "Trip saved successfully", Toast.LENGTH_SHORT).show();

                // Return to home screen or trip list
                if (getActivity() != null) {
                    // Navigate back to home fragment
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commit();
                }
            }

            @Override
            public void onFailure(Exception e) {
                btnSaveTrip.setEnabled(true);
                btnSaveTrip.setText("Save Trip");
                Toast.makeText(requireContext(), "Error saving trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTripData(String tripId) {
        firestoreManager.getTripById(tripId, new FirestoreManager.DataCallback<Trip>() {
            @Override
            public void onSuccess(Trip result) {
                if (result != null) {
                    currentTrip = result;
                    populateFormWithTripData(result);
                } else {
                    Toast.makeText(requireContext(), "Trip not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Error loading trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateFormWithTripData(Trip trip) {
        etTripName.setText(trip.getName());
        etDestination.setText(trip.getDestination());

        // Format dates
        if (trip.getStartDate() != null) {
            etStartDate.setText(dateFormat.format(trip.getStartDate()));
        }
        if (trip.getEndDate() != null) {
            etEndDate.setText(dateFormat.format(trip.getEndDate()));
        }

        etFlightNumber.setText(trip.getFlightNumber());

        if (trip.getBudget() > 0) {
            etBudget.setText(String.valueOf(trip.getBudget()));
        }

        dropdownCurrency.setText(trip.getCurrency(), false);
        etNotes.setText(trip.getNotes());

        // Load image if available
        if (trip.getImageUrl() != null && !trip.getImageUrl().isEmpty()) {
            // You'd typically use Glide or Picasso here to load the image
            // For simplicity, we'll just note that this would be needed
            tripImage.setVisibility(View.VISIBLE);
            // Example with Glide:
            // Glide.with(this).load(trip.getImageUrl()).into(tripImage);
        }
        if (trip.getLocations() != null && !trip.getLocations().isEmpty()) {
            // Clear any existing data
            locationsByDay.clear();
            daysContainer.removeAllViews();

            // Group locations by day
            int maxDay = 1;
            for (Location location : trip.getLocations()) {
                int dayIndex = location.getDayIndex();
                if (dayIndex > maxDay) {
                    maxDay = dayIndex;
                }

                if (!locationsByDay.containsKey(dayIndex)) {
                    locationsByDay.put(dayIndex, new ArrayList<>());
                }
                locationsByDay.get(dayIndex).add(location);
            }

            // Create day containers
            for (int i = 1; i <= maxDay; i++) {
                addDayContainer(i);
            }

            // Update day count
            dayCount = maxDay;
        }

        if (trip.getDocuments() != null && !trip.getDocuments().isEmpty()) {
            documentList.clear();
            documentList.addAll(trip.getDocuments());
            documentAdapter.setDocuments(documentList);
            rvDocuments.setVisibility(View.VISIBLE);
        }

        // Update button text for edit mode
        btnSaveTrip.setText("Update Trip");
    }

    private void setupPlaceAutocomplete() {
        try {
            // Create adapter with inline callback
            destinationAdapter = new PlacesAutocompleteAdapter(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    place -> {
                        etDestination.setText(place.getName());
                        etDestination.dismissDropDown();

                        // Store latitude and longitude
                        if (place.getLatLng() != null) {
                            selectedLat = place.getLatLng().latitude;
                            selectedLng = place.getLatLng().longitude;
                        }
                    });

            // Set adapter to AutoCompleteTextView
            etDestination.setAdapter(destinationAdapter);
            etDestination.setThreshold(2);

            Log.d("TripPlanningFragment", "Places adapter setup successfully for destination");
        } catch (Exception e) {
            Log.e("TripPlanningFragment", "Error setting up Places adapter for destination: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up Places autocomplete", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPlaceSelected(Place place) {
        etDestination.setText(place.getName());

        // Store latitude and longitude
        if (place.getLatLng() != null) {
            selectedLat = place.getLatLng().latitude;
            selectedLng = place.getLatLng().longitude;
        }

        // Hide the dropdown
        ((AutoCompleteTextView) etDestination).dismissDropDown();
    }

    private void setupLocationManagement() {
        // Add first day
        addDayContainer(1);

        // Setup add day button
        btnAddDay.setOnClickListener(v -> {
            dayCount++;
            addDayContainer(dayCount);
        });
    }

    private void addDayContainer(int dayIndex) {
        // Inflate day container view
        View dayView = getLayoutInflater().inflate(R.layout.item_day_locations, daysContainer, false);

        // Setup day title
        TextView tvDayTitle = dayView.findViewById(R.id.tv_day_title);
        tvDayTitle.setText("Locations for Day " + dayIndex);

        // Setup RecyclerView
        RecyclerView rvLocations = dayView.findViewById(R.id.rv_locations);
        rvLocations.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create adapter for this day
        LocationAdapter adapter = new LocationAdapter(requireContext(), dayIndex,
                new LocationAdapter.LocationAdapterListener() {
            @Override
            public void onEditLocation(Location location, int position, int dayIndex) {
                showLocationDialog(location, position, dayIndex);
            }

            @Override
            public void onDeleteLocation(Location location, int position, int dayIndex) {
                confirmDeleteLocation(location, position, dayIndex);
            }
        });

        rvLocations.setAdapter(adapter);

        // Initialize empty location list for this day
        if (!locationsByDay.containsKey(dayIndex)) {
            locationsByDay.put(dayIndex, new ArrayList<>());
        }

        // Set data to adapter
        adapter.setLocations(locationsByDay.get(dayIndex));

        // Setup add location button
        Button btnAddLocation = dayView.findViewById(R.id.btn_add_location);
        btnAddLocation.setOnClickListener(v -> {
            showLocationDialog(null, -1, dayIndex);
        });

        // Store references
        dayRecyclerViews.put(dayIndex, rvLocations);
        locationAdapters.put(dayIndex, adapter);

        // Add to container
        daysContainer.addView(dayView);
    }

    private void showLocationDialog(Location location, int position, int dayIndex) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_location, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        // Get views
        TextView tvDialogTitle = dialogView.findViewById(R.id.tv_dialog_title);
        AutoCompleteTextView etLocationName = dialogView.findViewById(R.id.et_location_name);
        TextInputEditText etStartTime = dialogView.findViewById(R.id.et_start_time);
        TextInputEditText etEndTime = dialogView.findViewById(R.id.et_end_time);
        TextInputEditText etLocationNotes = dialogView.findViewById(R.id.et_location_notes);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnSaveLocation = dialogView.findViewById(R.id.btn_save_location);

        // Set autocomplete adapter for location name
        PlacesAutocompleteAdapter dialogPlacesAdapter = new PlacesAutocompleteAdapter(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                place -> {
                    etLocationName.setText(place.getName());
                    etLocationName.dismissDropDown();

                    // Store the latitude and longitude for this place
                    if (place.getLatLng() != null) {
                        etLocationName.setTag(place.getLatLng());
                    }
                });

        etLocationName.setAdapter(dialogPlacesAdapter);
        etLocationName.setThreshold(2);

        // Make sure to clean up when dialog closes
        builder.setOnDismissListener(dialogInterface -> {
            dialogPlacesAdapter.shutdown();
        });

        // Add text watchers to validate and auto-format time input
        etStartTime.addTextChangedListener(new TimeFormatWatcher(etStartTime));
        etEndTime.addTextChangedListener(new TimeFormatWatcher(etEndTime));

        // If editing, populate fields
        if (location != null) {
            tvDialogTitle.setText("Edit Location");
            etLocationName.setText(location.getName());
            etStartTime.setText(location.getStartTime());
            etEndTime.setText(location.getEndTime());
            etLocationNotes.setText(location.getNotes());
        } else {
            tvDialogTitle.setText("Add Location");
        }

        AlertDialog dialog = builder.create();

        // Handle button clicks
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSaveLocation.setOnClickListener(v -> {
            String name = etLocationName.getText().toString().trim();
            if (name.isEmpty()) {
                etLocationName.setError("Location name is required");
                return;
            }

            // Validate start and end times
            String startTime = etStartTime.getText().toString().trim();
            String endTime = etEndTime.getText().toString().trim();

            if (!startTime.isEmpty() && !isValidTimeFormat(startTime)) {
                etStartTime.setError("Use format HH:MM (e.g. 09:30)");
                return;
            }

            if (!endTime.isEmpty() && !isValidTimeFormat(endTime)) {
                etEndTime.setError("Use format HH:MM (e.g. 14:45)");
                return;
            }

            // Create or update location
            Location newLocation = (location != null) ? location : new Location();
            newLocation.setName(name);
            newLocation.setStartTime(startTime);
            newLocation.setEndTime(endTime);
            newLocation.setNotes(etLocationNotes.getText().toString().trim());

            // Set latitude and longitude if available from Places API
            if (etLocationName.getTag() instanceof com.google.android.gms.maps.model.LatLng) {
                com.google.android.gms.maps.model.LatLng latLng
                        = (com.google.android.gms.maps.model.LatLng) etLocationName.getTag();
                newLocation.setLatitude(latLng.latitude);
                newLocation.setLongitude(latLng.longitude);
            }

            // Save to appropriate day's list
            if (position == -1) {
                // Add new location
                locationAdapters.get(dayIndex).addLocation(newLocation);
                locationsByDay.get(dayIndex).add(newLocation);
            } else {
                // Update existing location
                locationAdapters.get(dayIndex).updateLocation(newLocation, position);
                locationsByDay.get(dayIndex).set(position, newLocation);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void confirmDeleteLocation(Location location, int position, int dayIndex) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Location")
                .setMessage("Are you sure you want to delete " + location.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    locationAdapters.get(dayIndex).removeLocation(position);
                    locationsByDay.get(dayIndex).remove(position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean isValidTimeFormat(String time) {
        if (time == null || time.isEmpty()) {
            return true; // Empty is considered valid (optional field)
        }

        // Regex for HH:MM format (00:00 to 23:59)
        String timePattern = "^([01]?[0-9]|2[0-3]):([0-5][0-9])$";
        return time.matches(timePattern);
    }

    private class TimeFormatWatcher implements TextWatcher {

        private final TextInputEditText timeField;
        private boolean mChanging = false;

        public TimeFormatWatcher(TextInputEditText timeField) {
            this.timeField = timeField;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not needed
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Not needed
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mChanging) {
                return;
            }

            String text = s.toString().trim();
            if (text.isEmpty()) {
                return;
            }

            // If it already contains a colon, check if it's valid
            if (text.contains(":")) {
                if (!isValidTimeFormat(text)) {
                    timeField.setError("Use format HH:MM (e.g. 09:30)");
                } else {
                    timeField.setError(null);
                }
                return;
            }

            // If it's a numeric entry without colon, try to auto-format it
            if (text.matches("\\d+")) {
                mChanging = true;

                try {
                    // If we have 4 digits, format it as HH:MM
                    if (text.length() == 4) {
                        int hours = Integer.parseInt(text.substring(0, 2));
                        int minutes = Integer.parseInt(text.substring(2, 4));

                        // Validate hours and minutes
                        if (hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59) {
                            // Format as HH:MM
                            String formattedTime = String.format(Locale.US, "%02d:%02d", hours, minutes);
                            timeField.setText(formattedTime);
                            timeField.setSelection(formattedTime.length());
                            timeField.setError(null);
                        } else {
                            timeField.setError("Invalid time. Hours: 00-23, Minutes: 00-59");
                        }
                    } // If we have fewer than 4 digits, wait for more input
                    else if (text.length() < 4) {
                        // Just clear any error while typing
                        timeField.setError(null);
                    } // If we have more than 4 digits, show an error
                    else if (text.length() > 4) {
                        timeField.setError("Enter exactly 4 digits (e.g., 1230 for 12:30)");
                    }
                } catch (NumberFormatException e) {
                    timeField.setError("Invalid time format");
                }

                mChanging = false;
            }
        }
    }
}
