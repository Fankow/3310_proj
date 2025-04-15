package edu.cuhk.a3310_final_proj.network;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.a3310_final_proj.BuildConfig;
import edu.cuhk.a3310_final_proj.models.Airport;
import edu.cuhk.a3310_final_proj.models.Flight;
import edu.cuhk.a3310_final_proj.models.FlightSearchResponse;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FlightSearchClient {

    private static final String TAG = "FlightSearchClient";
    private static final String BASE_URL = "https://serpapi.com/";
    private static final String API_KEY = BuildConfig.SERP_API_KEY;

    private static FlightSearchClient instance;
    private final FlightSearchService service;

    private FlightSearchClient() {
        // Create logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Create OkHttpClient with interceptor
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        // Build Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create service
        service = retrofit.create(FlightSearchService.class);
    }

    public static synchronized FlightSearchClient getInstance() {
        if (instance == null) {
            instance = new FlightSearchClient();
        }
        return instance;
    }

    public void searchFlights(String departureId, String arrivalId, String outboundDate,
            final FlightSearchCallback callback) {
        Log.d(TAG, "Searching flights from " + departureId + " to " + arrivalId + " on " + outboundDate);

        // Make the API call
        Call<FlightSearchResponse> call = service.searchFlights(
                "google_flights",
                departureId,
                arrivalId,
                outboundDate,
                "en", // language
                "us", // country
                "USD", // currency
                "2", // type - for one way flights
                API_KEY
        );

        call.enqueue(new Callback<FlightSearchResponse>() {
            @Override
            public void onResponse(Call<FlightSearchResponse> call, Response<FlightSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Flight> flights = extractFlightsFromResponse(response.body());
                    callback.onSuccess(flights);
                } else {
                    String errorMsg = "Error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    Log.e(TAG, "API Error: " + errorMsg);
                    callback.onFailure(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<FlightSearchResponse> call, Throwable t) {
                Log.e(TAG, "Network error during flight search", t);
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    // To handle round trip, search for flights in reverse direction with the return date
    public void searchRoundTripFlights(String departureId, String arrivalId,
            String outboundDate, String returnDate,
            final RoundTripFlightSearchCallback callback) {

        // First, search for outbound flights
        searchFlights(departureId, arrivalId, outboundDate, new FlightSearchCallback() {
            @Override
            public void onSuccess(List<Flight> outboundFlights) {
                // Then search for return flights
                searchFlights(arrivalId, departureId, returnDate, new FlightSearchCallback() {
                    @Override
                    public void onSuccess(List<Flight> returnFlights) {
                        // Both searches successful, return both lists
                        callback.onSuccess(outboundFlights, returnFlights);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Return flight search failed
                        callback.onFailure("Return flight search failed: " + errorMessage);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                // Outbound flight search failed
                callback.onFailure("Outbound flight search failed: " + errorMessage);
            }
        });
    }

    private List<Flight> extractFlightsFromResponse(FlightSearchResponse response) {
        List<Flight> result = new ArrayList<>();
        boolean flightsFound = false;

        // Case 1: Extract from best_flights if available
        if (response.getBestFlights() != null && !response.getBestFlights().isEmpty()) {
            Log.d(TAG, "Found best_flights array with " + response.getBestFlights().size() + " entries");

            for (FlightSearchResponse.BestFlight bestFlight : response.getBestFlights()) {
                if (bestFlight.getFlights() != null && !bestFlight.getFlights().isEmpty()) {
                    Log.d(TAG, "Found " + bestFlight.getFlights().size() + " flights in best_flight");

                    // This is where the actual Flight objects are
                    for (Flight flight : bestFlight.getFlights()) {
                        // Log to check if flight number is present here
                        Log.d(TAG, "Extracted flight from best_flights: "
                                + (flight.getAirline() != null ? flight.getAirline() : "Unknown")
                                + " " + (flight.getFlightNumber() != null ? flight.getFlightNumber() : "unknown number"));

                        result.add(flight);
                        flightsFound = true;
                    }
                } else {
                    Log.d(TAG, "No flights array found in best_flight");
                }
            }
        } else {
            Log.d(TAG, "No best_flights array found in response");
        }

        // Case 2: If no flights found in best_flights, try direct flights array
        if (!flightsFound && response.getFlights() != null && !response.getFlights().isEmpty()) {
            Log.d(TAG, "Found direct flights array with " + response.getFlights().size() + " entries");

            for (Flight flight : response.getFlights()) {
                Log.d(TAG, "Extracted flight from direct flights: "
                        + (flight.getAirline() != null ? flight.getAirline() : "Unknown")
                        + " " + (flight.getFlightNumber() != null ? flight.getFlightNumber() : "unknown number"));

                result.add(flight);
            }
        } else if (!flightsFound) {
            Log.d(TAG, "No direct flights array found or it's empty");
        }


        return result;
    }

    public interface FlightSearchCallback {

        void onSuccess(List<Flight> flights);

        void onFailure(String errorMessage);
    }

    public interface RoundTripFlightSearchCallback {

        void onSuccess(List<Flight> outboundFlights, List<Flight> returnFlights);

        void onFailure(String errorMessage);
    }
}
