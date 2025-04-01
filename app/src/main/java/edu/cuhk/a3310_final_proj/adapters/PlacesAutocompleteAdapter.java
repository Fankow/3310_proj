package edu.cuhk.a3310_final_proj.adapters;

import android.content.Context;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlacesAutocompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {

    private static final String TAG = "PlacesAutocomplete";
    private List<AutocompletePrediction> predictions = new ArrayList<>();
    private PlacesClient placesClient;
    private AutocompleteSessionToken token;
    private PlaceAutocompleteListener listener;


    public interface PlaceAutocompleteListener {

        void onPlaceSelected(Place place);
    }

    public PlacesAutocompleteAdapter(@NonNull Context context, int resource, PlaceAutocompleteListener listener) {
        super(context, resource);
        this.placesClient = com.google.android.libraries.places.api.Places.createClient(context);
        this.token = AutocompleteSessionToken.newInstance();
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return predictions.size();
    }

    @Nullable
    @Override
    public AutocompletePrediction getItem(int position) {
        return predictions.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);

        AutocompletePrediction item = getItem(position);
        if (item != null) {
            textView.setText(item.getPrimaryText(null));
        }

        convertView.setOnClickListener(v -> {
            if (item != null) {
                // Get detailed place information
                String placeId = item.getPlaceId();
                List<Place.Field> placeFields = new ArrayList<>();
                placeFields.add(Place.Field.ID);
                placeFields.add(Place.Field.NAME);
                placeFields.add(Place.Field.ADDRESS);
                placeFields.add(Place.Field.LAT_LNG);

                com.google.android.libraries.places.api.net.FetchPlaceRequest request
                        = com.google.android.libraries.places.api.net.FetchPlaceRequest.newInstance(placeId, placeFields);

                placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                    Place place = response.getPlace();
                    if (listener != null) {
                        listener.onPlaceSelected(place);
                    }
                });
            }
        });

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                // No search term entered
                if (constraint == null || constraint.length() == 0) {
                    results.values = new ArrayList<AutocompletePrediction>();
                    results.count = 0;
                    return results;
                }

                List<AutocompletePrediction> filteredPredictions = getAutocomplete(constraint.toString());

                results.values = filteredPredictions;
                results.count = filteredPredictions.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    predictions.clear(); // Clear the old list
                    predictions.addAll((List<AutocompletePrediction>) results.values);
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    private List<AutocompletePrediction> getAutocomplete(String query) {
        Log.d(TAG, "Starting autocomplete query: " + query);

        // Create a request
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .build();

        Task<FindAutocompletePredictionsResponse> task = placesClient.findAutocompletePredictions(request);

        try {
            Tasks.await(task, 5, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "Error fetching autocomplete predictions", e);
            e.printStackTrace();
            return new ArrayList<>();
        }

        if (task.isSuccessful()) {
            FindAutocompletePredictionsResponse response = task.getResult();
            if (response != null) {
                List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                Log.d(TAG, "Autocomplete predictions found: " + predictions.size());
                return predictions;
            }
        } else {
            Exception exception = task.getException();
            Log.e(TAG, "Autocomplete task not successful: " + (exception != null ? exception.getMessage() : "Unknown error"));
        }

        return new ArrayList<>();
    }
    public void shutdown() {
        // No direct way to shut down PlacesClient, but we can clean up resources
        if (placesClient != null) {
            // Release references
            placesClient = null;
        }
    }
}
