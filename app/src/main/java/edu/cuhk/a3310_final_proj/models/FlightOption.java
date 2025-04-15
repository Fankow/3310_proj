package edu.cuhk.a3310_final_proj.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class FlightOption implements Serializable {

    private List<Flight> flights;
    private int totalDuration;
    private double price;
    private String type;
    private String airlineIcon;

    @SerializedName("flights")
    public List<Flight> getFlights() {
        return flights;
    }

    @SerializedName("flights")
    public void setFlights(List<Flight> flights) {
        this.flights = flights;
    }

    @SerializedName("total_duration")
    public int getTotalDuration() {
        return totalDuration;
    }

    @SerializedName("total_duration")
    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @SerializedName("airline_logo")
    public String getAirlineIcon() {
        return airlineIcon;
    }

    @SerializedName("airline_logo")
    public void setAirlineIcon(String airlineIcon) {
        this.airlineIcon = airlineIcon;
    }

    // Helper method to get formatted duration
    public String getFormattedTotalDuration() {
        int hours = totalDuration / 60;
        int minutes = totalDuration % 60;
        return hours + "h " + minutes + "m";
    }
}
