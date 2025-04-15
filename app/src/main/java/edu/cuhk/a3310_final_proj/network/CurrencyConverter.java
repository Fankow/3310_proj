package edu.cuhk.a3310_final_proj.network;

import android.util.Log;
import com.google.gson.annotations.SerializedName;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import java.util.HashMap;
import java.util.Map;
import edu.cuhk.a3310_final_proj.BuildConfig;

public class CurrencyConverter {

    public static final String BASE_URL = "https://api.exchangerate-api.com/v4/latest/";
    public static final String API_KEY = BuildConfig.EXCHANGE_RATES_API_KEY;
    private static final int CACHE_DURATION = 30 * 60 * 1000; // 30 minutes
    private static Map<String, CachedRate> rateCache = new HashMap<>();

    public static Call<ExchangeRateResponse> convert(
            String from,
            String to,
            double amount,
            boolean forceUpdate,
            Callback callback
    ) {
        String cacheKey = from + "_" + to;

        // Check cache first
        if (!forceUpdate && rateCache.containsKey(cacheKey)) {
            CachedRate cached = rateCache.get(cacheKey);
            if (System.currentTimeMillis() - cached.timestamp < CACHE_DURATION) {
                double result = amount * cached.rate;
                callback.onSuccess(result);
                return null;
            }
        }

        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        CurrencyService service = retrofit.create(CurrencyService.class);
        Call<ExchangeRateResponse> call = service.getRates(from, API_KEY);

        call.enqueue(new retrofit2.Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double rate = response.body().getRates().get(to);
                    if (rate != null) {
                        rateCache.put(cacheKey, new CachedRate(rate, System.currentTimeMillis()));
                        double result = amount * rate;
                        callback.onSuccess(result);
                    } else {
                        callback.onFailure("Currency rate not found");
                    }
                } else {
                    callback.onFailure("API Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                if (!call.isCanceled()) {
                    callback.onFailure("Network Error: " + t.getMessage());
                }
            }
        });

        return call;
    }

    private static class CachedRate {

        double rate;
        long timestamp;

        CachedRate(double rate, long timestamp) {
            this.rate = rate;
            this.timestamp = timestamp;
        }
    }

    public interface Callback {

        void onSuccess(double result);

        void onFailure(String error);
    }

    public interface CurrencyService {

        @GET("{base}")
        Call<ExchangeRateResponse> getRates(
                @Path("base") String baseCurrency,
                @Query("api_key") String apiKey
        );
    }
}
