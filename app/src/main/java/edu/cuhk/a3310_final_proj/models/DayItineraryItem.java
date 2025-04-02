package edu.cuhk.a3310_final_proj.models;

import java.util.Date;
import java.util.List;

public class DayItineraryItem {

    private String dayTitle;
    private Date date;
    private List<Location> locations;

    public DayItineraryItem(String dayTitle, Date date, List<Location> locations) {
        this.dayTitle = dayTitle;
        this.date = date;
        this.locations = locations;
    }

    public String getDayTitle() {
        return dayTitle;
    }

    public Date getDate() {
        return date;
    }

    public List<Location> getLocations() {
        return locations;
    }
}
