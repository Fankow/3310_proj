package edu.cuhk.a3310_final_proj.network;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ExchangeRateResponse {
    private Map<String, Double> rates;
    @SerializedName("rates")

    public Map<String, Double> getRates() {
        return rates;
    }
}