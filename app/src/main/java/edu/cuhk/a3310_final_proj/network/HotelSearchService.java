package edu.cuhk.a3310_final_proj.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface HotelSearchService {

    // SerpAPI Google Hotels endpoint
    @GET("search.json")
    Call<HotelSearchResponse> searchHotels(
            @Query("engine") String engine,
            @Query("q") String query,
            @Query("check_in_date") String checkInDate,
            @Query("check_out_date") String checkOutDate,
            @Query("api_key") String apiKey
    );
}
