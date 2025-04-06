package edu.cuhk.a3310_final_proj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.TripAdapter;
import edu.cuhk.a3310_final_proj.firebase.FirestoreManager;
import edu.cuhk.a3310_final_proj.firebase.StorageManager;
import edu.cuhk.a3310_final_proj.models.Trip;

public class TripViewFragment extends Fragment {

    private StorageManager storageManager;
    private FirestoreManager firestoreManager;
    private RecyclerView tripRecyclerView;
    private TripAdapter tripAdapter;
    private List<Trip> tripList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        storageManager = StorageManager.getInstance();
        firestoreManager = FirestoreManager.getInstance();
        return inflater.inflate(R.layout.fragment_trip_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize RecyclerView
        tripRecyclerView = view.findViewById(R.id.trips_recycler_view);
        tripRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create and set adapter
        tripAdapter = new TripAdapter(requireContext(), tripList, new TripAdapter.TripAdapterListener() {
            @Override
            public void onTripClicked(Trip trip) {
                // Navigate to TripDetailFragment or show detail view
                showTripDetails(trip);
            }

            @Override
            public void onEditTrip(Trip trip) {
                // Navigate to TripPlanningFragment with the trip ID for editing
                openTripEditor(trip);
            }

            @Override
            public void onDeleteTrip(Trip trip) {
                // Show confirmation dialog and delete trip
                confirmDeleteTrip(trip);
            }
        });

        tripRecyclerView.setAdapter(tripAdapter);

        // Load trips for the current user
        loadUserTrips();
    }

    private void loadUserTrips() {
        // Show loading indicator if you have one

        // Log the attempt
        Log.d("TripViewFragment", "Loading trips for user");

        // Reuse the FirestoreManager's getUserTrips method
        firestoreManager.getUserTrips(new FirestoreManager.DataCallback<List<Trip>>() {
            @Override
            public void onSuccess(List<Trip> result) {
                // Log the result
                Log.d("TripViewFragment", "Loaded " + (result != null ? result.size() : 0) + " trips");

                // Update adapter with the trip list
                tripList.clear();
                if (result != null && !result.isEmpty()) {
                    tripList.addAll(result);
                    tripAdapter.notifyDataSetChanged();
                    showEmptyState(false);
                } else {
                    // Show empty state view if needed
                    Log.d("TripViewFragment", "No trips found, showing empty state");
                    showEmptyState(true);
                }

                // Hide loading indicator if you have one
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("TripViewFragment", "Failed to load trips: " + e.getMessage(), e);

                // Show a more user-friendly error message
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("FAILED_PRECONDITION")
                        && errorMessage.contains("requires an index")) {
                    errorMessage = "Database setup incomplete. Please try refreshing or contact support.";
                }

                Toast.makeText(requireContext(),
                        "Failed to load trips: " + errorMessage, Toast.LENGTH_SHORT).show();

                // Show empty state with error
                showEmptyStateWithError();

                // Hide loading indicator if you have one
            }
        });
    }

    private void showTripDetails(Trip trip) {
        // Create a new fragment to show trip details
        Fragment tripDetailFragment = new TripDetailFragment();

        // Pass the trip ID as argument
        Bundle args = new Bundle();
        args.putString("trip_id", trip.getId());
        tripDetailFragment.setArguments(args);

        // Navigate to the trip detail fragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, tripDetailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openTripEditor(Trip trip) {
        // Create a new fragment to edit the trip
        Fragment tripPlanningFragment = new TripPlanningFragment();

        // Pass the trip ID as argument
        Bundle args = new Bundle();
        args.putString("trip_id", trip.getId());
        tripPlanningFragment.setArguments(args);

        // Navigate to the trip planning fragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, tripPlanningFragment)
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteTrip(Trip trip) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete " + trip.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteTrip(trip);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTrip(Trip trip) {
        firestoreManager.deleteTrip(trip.getId(), new FirestoreManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Remove the trip from the list and update adapter
                int position = findTripPosition(trip.getId());
                if (position != -1) {
                    tripList.remove(position);
                    tripAdapter.notifyItemRemoved(position);

                    // Show empty state if no trips left
                    if (tripList.isEmpty()) {
                        showEmptyState(true);
                    }
                }

                Toast.makeText(requireContext(), "Trip deleted successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(),
                        "Failed to delete trip: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int findTripPosition(String tripId) {
        for (int i = 0; i < tripList.size(); i++) {
            if (tripList.get(i).getId().equals(tripId)) {
                return i;
            }
        }
        return -1;
    }

    private void showEmptyState(boolean show) {
        // Implement to show/hide empty state view
        // For example:
        // emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        // tripRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        return;
    }

    private void showEmptyStateWithError() {
        // Implement to show/hide empty state view with error
        // For example:
        // emptyStateView.setVisibility(View.VISIBLE);
        // errorTextView.setVisibility(View.VISIBLE);
        // tripRecyclerView.setVisibility(View.GONE);
        return;
    }
}
