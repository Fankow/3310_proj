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

    private View view;
    private Button planTripButton;        // Button to create a new trip
    private Button viewMoreButton;        // Button to view all trips
    private RecyclerView tripsRecyclerView; // RecyclerView to display trips preview

    // Adapter and data for the RecyclerView
    private HomeTripAdapter tripAdapter;
    private List<Trip> tripList = new ArrayList<>();

    // Firebase manager for fetching trip data
    private FirestoreManager firestoreManager;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;

        firestoreManager = FirestoreManager.getInstance();

        initializeViews();

        //load trip for homepage
        loadRecentTrips();
    }


    private void initializeViews() {
        planTripButton = view.findViewById(R.id.btn_plan_new_trip);
        planTripButton.setOnClickListener(v -> {
            Fragment tripPlanningFragment = new TripPlanningFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, tripPlanningFragment)
                    .addToBackStack(null) // This allows the user to go back
                    .commit();
        });

        viewMoreButton = view.findViewById(R.id.trip_view_more);
        viewMoreButton.setOnClickListener(v -> {
            Fragment tripViewFragment = new TripViewFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, tripViewFragment)
                    .addToBackStack(null)
                    .commit();
        });

        tripsRecyclerView = view.findViewById(R.id.trips_recycler_view);
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        tripAdapter = new HomeTripAdapter(requireContext(), tripList, this::showTripDetails);
        tripsRecyclerView.setAdapter(tripAdapter);
    }

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

                    // Show "View More" button only if there are more trips than MAX_TRIPS_TO_DISPLAY
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
                Log.e(TAG, "Failed to load trips: " + e.getMessage(), e);
                Toast.makeText(requireContext(),
                        "Failed to load trips: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                tripsRecyclerView.setVisibility(View.GONE);
                viewMoreButton.setVisibility(View.GONE);
            }
        });
    }
        private void showTripDetails(Trip trip) {
        Fragment tripDetailFragment = new TripDetailFragment();

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
