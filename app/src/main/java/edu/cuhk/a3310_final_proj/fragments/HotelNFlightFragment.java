package edu.cuhk.a3310_final_proj.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.LocationBias;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.cuhk.a3310_final_proj.R;
import edu.cuhk.a3310_final_proj.adapters.HotelAdapter;
import edu.cuhk.a3310_final_proj.adapters.PlacesAutocompleteAdapter;
import edu.cuhk.a3310_final_proj.models.Hotel;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;

public class HotelNFlightFragment extends Fragment {

    private AutoCompleteTextView etLocation;
    private PlacesAutocompleteAdapter locationAdapter;
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private HotelAdapter hotelAdapter;
    private List<Hotel> hotels = new ArrayList<>();
    private PlacesClient placesClient;
    private ChipGroup categoryChips;
    private Chip hotelChip;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return getLayoutInflater().inflate(R.layout.fragment_hotel_flight, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_api_key));
        }
        etLocation = view.findViewById(R.id.search_input);
        recyclerView = view.findViewById(R.id.results_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        categoryChips = view.findViewById(R.id.category_chips);
        placesClient = Places.createClient(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        hotelAdapter = new HotelAdapter(requireContext(), hotels);
        recyclerView.setAdapter(hotelAdapter);
        setupPlaceAutocomplete();

        hotelChip = view.findViewById(R.id.chip_hotel);
        hotelChip.setOnClickListener(v -> {
            if (!etLocation.getText().toString().isEmpty()) {
                searchForLocation(etLocation.getText().toString());
            }
        });
    }

    private void setupPlaceAutocomplete() {
        try {
            locationAdapter = new PlacesAutocompleteAdapter(requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    place -> {
                        etLocation.setText(place.getName());
                        etLocation.dismissDropDown();
                    });
            etLocation.setAdapter(locationAdapter);
            etLocation.setThreshold(2);
            Log.d("HotelNFlightFragment", "Places adapter setup successfully for location");
        } catch (Exception e) {
            Log.e("HotelNFlightFragment", "Error setting up Places adapter for location" + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up Places autocomplete", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchForLocation(String locationName) {
        if (placesClient == null) {
            Log.e("HotelNFlightFragment", "PlacesClient is null");
            emptyStateText.setText("Error: Places API not initialized");

            // Attempt to recover by re-initializing
            if (!Places.isInitialized()) {
                Places.initialize(requireContext(), getString(R.string.google_maps_api_key));
            }
            placesClient = Places.createClient(requireContext());

            if (placesClient == null) {
                return; // Still null, can't proceed
            }
        }

        // Show loading state
        emptyStateText.setText("Searching...");
        emptyStateText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(locationName)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    if (response.getAutocompletePredictions().isEmpty()) {
                        emptyStateText.setText("No locations found matching: " + locationName);
                        return;
                    }

                    AutocompletePrediction prediction = response.getAutocompletePredictions().get(0);
                    String placeId = prediction.getPlaceId();

                    // Actually call the method to fetch place details
                    fetchPlaceDetails(placeId);
                })
                .addOnFailureListener(exception -> {
                    emptyStateText.setText("Error finding location: " + exception.getMessage());
                    Log.e("HotelNFlightFragment", "Error finding predictions", exception);
                });
    }

//    private void fetchPlaceDetails(String placeId) {
//        List<Place.Field> placeFields = Arrays.asList(
//                Place.Field.ID,
//                Place.Field.NAME,
//                Place.Field.LAT_LNG);
//        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
//        placesClient.fetchPlace(request)
//                .addOnSuccessListener(response -> {
//                    Place place = response.getPlace();
//                    LatLng location = place.getLatLng();
//                    if (location != null) {
//                        searchHotelsNearby(location, place.getName());
//                    } else {
//                        emptyStateText.setText("Could not detemine the location coordinates");
//                    }
//                })
//                .addOnFailureListener(exception -> {
//                    emptyStateText.setText("Error fetching place details: " + exception.getMessage());
//                    Log.e("HotelNFlightFragment", "Error fetching place details", exception);
//                });
//    }

    private void searchHotelsNearby(LatLng location, String locationName) {
        // Define search bounds (approximately 5km radius)
        double radiusInMeters = 5000.0;
        CircularBounds bounds = CircularBounds.newInstance(location, radiusInMeters);

        // Define the fields you need for each Place
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        );

        // Build the SearchNearbyRequest with optional parameters.
        // For example, include "lodging" and "hotel" while excluding "motel",
        // limit the result to 10, rank by distance and set region code to US.
        SearchNearbyRequest request = SearchNearbyRequest.builder(bounds, fields)
                .setIncludedTypes(Arrays.asList("lodging", "hotel"))
                .setExcludedTypes(Arrays.asList("motel"))
                .setMaxResultCount(20)
                .setRankPreference(SearchNearbyRequest.RankPreference.DISTANCE)
                //.setRegionCode("US")
                .build();

        // Execute the SearchNearbyRequest
        placesClient.searchNearby(request)
                .addOnSuccessListener(response -> {
                    hotels.clear();
                    List<Place> places = response.getPlaces();

                    if (places.isEmpty()) {
                        emptyStateText.setText("No hotels found near " + locationName);
                        emptyStateText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        return;
                    }

                    // Process each Place and fetch details
                    for (int i = 0; i < places.size(); i++) {
                        Place place = places.get(i);
                        // Use fetchHotelDetails for each hotel; mark last if it's the final one.
                        fetchHotelDetails(place.getId(), i == places.size() - 1);
                    }
                })
                .addOnFailureListener(exception -> {
                    emptyStateText.setText("Error searching for hotels: " + exception.getMessage());
                    emptyStateText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Log.e("HotelNFlightFragment", "Error in searchNearby", exception);
                });
    }
    private void searchHotelsByKeyword(LatLng location, String locationName) {
        // Define rectangular bounds approximating a 5km radius
        double radiusInMeters = 5000000000.0;
        double latOffset = radiusInMeters ; // roughly convert meters to degrees latitude
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(location.latitude - latOffset, location.longitude - latOffset),
                new LatLng(location.latitude + latOffset, location.longitude + latOffset)
        );

        // Build an autocomplete request with a combined query string, e.g., "hotel in [locationName]"
        String query = "hotel in " + locationName;
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setLocationBias(bounds)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    hotels.clear();
                    List<AutocompletePrediction> predictions = response.getAutocompletePredictions();

                    if (predictions.isEmpty()) {
                        emptyStateText.setText("No hotels found near " + locationName);
                        emptyStateText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        return;
                    }

                    // Process each prediction by fetching its details
                    for (int i = 0; i < predictions.size(); i++) {
                        AutocompletePrediction prediction = predictions.get(i);
                        String placeId = prediction.getPlaceId();
                        fetchHotelDetails(placeId, i == predictions.size() - 1);
                    }
                })
                .addOnFailureListener(exception -> {
                    emptyStateText.setText("Error searching for hotels: " + exception.getMessage());
                    emptyStateText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Log.e("HotelNFlightFragment", "Error in hotel keyword search", exception);
                });
    }
    private void fetchHotelDetails(String placeId, boolean isLast) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.RATING,
                Place.Field.LAT_LNG);

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();

                    Hotel hotel = new Hotel();
                    hotel.setName(place.getName());
                    hotel.setAddress(place.getAddress());
                    if (place.getLatLng() != null) {
                        hotel.setLatitude(place.getLatLng().latitude);
                        hotel.setLongitude(place.getLatLng().longitude);
                    }

                    hotels.add(hotel);

                    // Update UI if this is the last hotel or we've collected all hotels
                    if (isLast) {
                        emptyStateText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        hotelAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e("HotelNFlightFragment", "Error fetching hotel details", exception);

                    // Even if one fails, still show results if we have some
                    if (isLast && !hotels.isEmpty()) {
                        emptyStateText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        hotelAdapter.notifyDataSetChanged();
                    } else if (isLast) {
                        emptyStateText.setText("Error retrieving hotel information");
                    }
                });
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG);
        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng location = place.getLatLng();
                    if (location != null) {
                        // Call new searchHotelsByKeyword instead of searchHotelsNearby
                        searchHotelsByKeyword(location, place.getName());
                    } else {
                        emptyStateText.setText("Could not determine the location coordinates");
                    }
                })
                .addOnFailureListener(exception -> {
                    emptyStateText.setText("Error fetching place details: " + exception.getMessage());
                    Log.e("HotelNFlightFragment", "Error fetching place details", exception);
                });
    }
}
