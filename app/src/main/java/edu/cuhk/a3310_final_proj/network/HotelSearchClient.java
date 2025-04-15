package edu.cuhk.a3310_final_proj.network;

import android.util.Log;

import java.util.List;

import edu.cuhk.a3310_final_proj.BuildConfig;
import edu.cuhk.a3310_final_proj.models.Hotel;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HotelSearchClient {

    private static final String TAG = "HotelSearchClient";
    private static final String BASE_URL = "https://serpapi.com/";
    private static final String API_KEY = BuildConfig.SERP_API_KEY;

    private static HotelSearchClient instance;
    private final HotelSearchService service;

    private HotelSearchClient() {
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
        service = retrofit.create(HotelSearchService.class);
    }

    public static synchronized HotelSearchClient getInstance() {
        if (instance == null) {
            instance = new HotelSearchClient();
        }
        return instance;
    }

    public void searchHotels(String location, String checkInDate, String checkOutDate, final HotelSearchCallback callback) {
        // SerpAPI expects search query in the format: "hotels in {location}"
        String query = "hotels in " + location;

        // Make the API call with correct engine parameter
        Call<HotelSearchResponse> call = service.searchHotels(
                "google_hotels", // Use Google Hotels engine
                query,
                checkInDate,
                checkOutDate,
                API_KEY
        );

        call.enqueue(new Callback<HotelSearchResponse>() {
            @Override
            public void onResponse(Call<HotelSearchResponse> call, Response<HotelSearchResponse> response) {
                // Rest of the implementation remains the same
                if (response.isSuccessful() && response.body() != null) {
                    List<Hotel> hotels = response.body().getHotels();
                    callback.onSuccess(hotels);
                } else {
                    String errorMsg = "Error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            errorMsg = response.errorBody().string();
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onFailure(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<HotelSearchResponse> call, Throwable t) {
                Log.e(TAG, "Network error during hotel search", t);
                callback.onFailure("Network error: " + t.getMessage());
            }
        });
    }

    public interface HotelSearchCallback {

        void onSuccess(List<Hotel> hotels);

        void onFailure(String errorMessage);
    }
}
