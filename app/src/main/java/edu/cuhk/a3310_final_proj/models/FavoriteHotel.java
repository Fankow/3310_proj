package edu.cuhk.a3310_final_proj.models;

import com.google.firebase.firestore.PropertyName;
import java.util.Date;

public class FavoriteHotel {

    private String id;
    private String userId;
    private String hotelId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private String description;
    private Date savedAt;

    public FavoriteHotel() {
        // Required empty constructor for Firestore
        this.savedAt = new Date();
    }

    public FavoriteHotel(String userId, Hotel hotel) {
        this.userId = userId;
        this.hotelId = hotel.getName() + "_" + hotel.getLatitude() + "_" + hotel.getLongitude();
        this.name = hotel.getName();
        this.address = hotel.getAddress();
        this.latitude = hotel.getLatitude();
        this.longitude = hotel.getLongitude();
        this.imageUrl = hotel.getImageUrl();
        this.description = hotel.getDescription();
        this.savedAt = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("user_id")
    public String getUserId() {
        return userId;
    }

    @PropertyName("user_id")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("hotel_id")
    public String getHotelId() {
        return hotelId;
    }

    @PropertyName("hotel_id")
    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("saved_at")
    public Date getSavedAt() {
        return savedAt;
    }

    @PropertyName("saved_at")
    public void setSavedAt(Date savedAt) {
        this.savedAt = savedAt;
    }
}
