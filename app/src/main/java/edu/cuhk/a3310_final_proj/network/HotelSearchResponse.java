package edu.cuhk.a3310_final_proj.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;
import edu.cuhk.a3310_final_proj.models.Hotel;

public class HotelSearchResponse {

    @SerializedName("properties")
    private List<Hotel> hotels;

    public List<Hotel> getHotels() {
        return hotels != null ? hotels : new ArrayList<>();
    }
}
