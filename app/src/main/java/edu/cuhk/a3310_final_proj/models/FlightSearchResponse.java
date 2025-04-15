package edu.cuhk.a3310_final_proj.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FlightSearchResponse {

    @SerializedName("best_flights")
    private List<BestFlight> best_flights;

    @SerializedName("flights")
    private List<Flight> flights;

    @SerializedName("search_parameters")
    private SearchParameters search_parameters;

    @SerializedName("best_flights")
    public List<BestFlight> getBestFlights() {
        return best_flights;
    }

    @SerializedName("best_flights")
    public void setBestFlights(List<BestFlight> best_flights) {
        this.best_flights = best_flights;
    }

    @SerializedName("flights")
    public List<Flight> getFlights() {
        return flights;
    }

    @SerializedName("flights")
    public void setFlights(List<Flight> flights) {
        this.flights = flights;
    }

    @SerializedName("search_parameters")
    public SearchParameters getSearchParameters() {
        return search_parameters;
    }

    @SerializedName("search_parameters")
    public void setSearchParameters(SearchParameters search_parameters) {
        this.search_parameters = search_parameters;
    }

    public static class BestFlight {

        private List<Flight> flights;

        public List<Flight> getFlights() {
            return flights;
        }

        public void setFlights(List<Flight> flights) {
            this.flights = flights;
        }
    }

    public static class SearchParameters {

        @SerializedName("departure_id")
        private String departure_id;

        @SerializedName("arrival_id")
        private String arrival_id;

        @SerializedName("outbound_date")
        private String outbound_date;

        public String getDepartureId() {
            return departure_id;
        }

        public String getArrivalId() {
            return arrival_id;
        }

        public String getOutboundDate() {
            return outbound_date;
        }
    }
}
