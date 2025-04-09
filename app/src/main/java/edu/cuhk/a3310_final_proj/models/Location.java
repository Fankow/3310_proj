package edu.cuhk.a3310_final_proj.models;

import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class Location {

    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int dayIndex = 1;
    @PropertyName("visit_date")
    private Date visitDate;

    @PropertyName("start_time")
    private String startTime;

    @PropertyName("end_time")
    private String endTime;

    private String notes;

    public Location() {
    }

    public Location(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @PropertyName("visit_date")
    public Date getVisitDate() {
        return visitDate;
    }

    @PropertyName("visit_date")
    public void setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
    }

    @PropertyName("start_time")
    public String getStartTime() {
        return startTime;
    }

    @PropertyName("start_time")
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    @PropertyName("end_time")
    public String getEndTime() {
        return endTime;
    }

    @PropertyName("end_time")
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }
}
