package edu.cuhk.a3310_final_proj.network;

import edu.cuhk.a3310_final_proj.models.FlightSearchResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FlightSearchService {

    @GET("search")
    Call<FlightSearchResponse> searchFlights(
            @Query("engine") String engine,
            @Query("departure_id") String departureId,
            @Query("arrival_id") String arrivalId,
            @Query("outbound_date") String outboundDate,
            @Query("hl") String language,
            @Query("gl") String country,
            @Query("currency") String currency,
            @Query("type") String type,
            @Query("api_key") String apiKey
    );
}
