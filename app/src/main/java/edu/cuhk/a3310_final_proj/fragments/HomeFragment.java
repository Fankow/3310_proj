package edu.cuhk.a3310_final_proj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.HomeTripAdapter;
import edu.cuhk.a3310_final_proj.firebase.FirestoreManager;
import edu.cuhk.a3310_final_proj.models.Trip;

/**
 * HomeFragment displays the main home screen of the app. It shows a preview of
 * the user's trips (limited to 3) with a "View More" button to see all trips,
 * and a button to plan a new trip.
 */
public class HomeFragment extends Fragment {

    // Tag for logging
    private static final String TAG = "HomeFragment";

    // Maximum number of trips to display on the home screen
    private static final int MAX_TRIPS_TO_DISPLAY = 3;

    // UI components
    private View view;
    private Button planTripButton;        // Button to create a new trip
    private Button viewMoreButton;        // Button to view all trips
    private RecyclerView tripsRecyclerView; // RecyclerView to display trips preview

    // Adapter and data for the RecyclerView
    private HomeTripAdapter tripAdapter;
    private List<Trip> tripList = new ArrayList<>();

    // Firebase manager for fetching trip data
    private FirestoreManager firestoreManager;

    /**
     * Inflates the layout for this fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    /**
     * Called after the view is created. Initializes components and loads data.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        // Initialize FirestoreManager for database operations
        firestoreManager = FirestoreManager.getInstance();

        // Initialize views and set up listeners
        initializeViews();

        // Load recent trips from Firebase
        loadRecentTrips();
    }

    /**
     * Initializes all UI components and sets up click listeners
     */
    private void initializeViews() {
        // Plan Trip button - navigates to trip planning screen
        planTripButton = view.findViewById(R.id.btn_plan_new_trip);
        planTripButton.setOnClickListener(v -> {
            Fragment tripPlanningFragment = new TripPlanningFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, tripPlanningFragment)
                    .addToBackStack(null) // This allows the user to go back
                    .commit();
        });

        // View More button - navigates to full trip list
        viewMoreButton = view.findViewById(R.id.trip_view_more);
        viewMoreButton.setOnClickListener(v -> {
            Fragment tripViewFragment = new TripViewFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, tripViewFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // RecyclerView for displaying trip previews
        tripsRecyclerView = view.findViewById(R.id.trips_recycler_view);
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create and set adapter - using simplified HomeTripAdapter which only allows clicking, not editing/deleting
        tripAdapter = new HomeTripAdapter(requireContext(), tripList, this::showTripDetails);
        tripsRecyclerView.setAdapter(tripAdapter);
    }

    /**
     * Loads a limited number of recent trips from Firestore Shows only
     * MAX_TRIPS_TO_DISPLAY trips for preview
     */
    private void loadRecentTrips() {
        // Load trips from Firestore database
        firestoreManager.getUserTrips(new FirestoreManager.DataCallback<List<Trip>>() {
            @Override
            public void onSuccess(List<Trip> result) {
                Log.d(TAG, "Loaded " + (result != null ? result.size() : 0) + " trips");

                tripList.clear();
                if (result != null && !result.isEmpty()) {
                    // Only add up to MAX_TRIPS_TO_DISPLAY trips
                    for (int i = 0; i < Math.min(result.size(), MAX_TRIPS_TO_DISPLAY); i++) {
                        tripList.add(result.get(i));
                    }

                    // Update UI to show trips
                    tripAdapter.notifyDataSetChanged();
                    tripsRecyclerView.setVisibility(View.VISIBLE);

                    // Show "View More" button only if there are more trips than we're displaying
                    viewMoreButton.setVisibility(result.size() > MAX_TRIPS_TO_DISPLAY
                            ? View.VISIBLE : View.GONE);
                } else {
                    // No trips to display
                    tripsRecyclerView.setVisibility(View.GONE);
                    viewMoreButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Log and show error message on failure
                Log.e(TAG, "Failed to load trips: " + e.getMessage(), e);
                Toast.makeText(requireContext(),
                        "Failed to load trips: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                // Hide trip-related UI elements
                tripsRecyclerView.setVisibility(View.GONE);
                viewMoreButton.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Opens the trip detail screen when a trip is clicked
     *
     * @param trip The trip to show details for
     */
    private void showTripDetails(Trip trip) {
        // Create trip detail fragment
        Fragment tripDetailFragment = new TripDetailFragment();

        // Pass trip ID as an argument
        Bundle args = new Bundle();
        args.putString("trip_id", trip.getId());
        tripDetailFragment.setArguments(args);

        // Navigate to trip detail screen
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, tripDetailFragment)
                .addToBackStack(null)
                .commit();
    }
}
