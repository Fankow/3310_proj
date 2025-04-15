package edu.cuhk.a3310_final_proj.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Hotel {

    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String imageUrl;
    private String description;
    private String rating;
    private String price;

    @SerializedName("gps_coordinates")
    private Coordinates coordinates;

    @SerializedName("rate_per_night")
    private RatePerNight ratePerNight;

    @SerializedName("images")
    private List<HotelImage> hotelImages;

    // Inner class for coordinates
    public static class Coordinates {

        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    // Inner class for rate information
    public static class RatePerNight {

        private String lowest;
        @SerializedName("extracted_lowest")
        private double extractedLowest;

        public String getLowest() {
            return lowest;
        }

        public double getExtractedLowest() {
            return extractedLowest;
        }
    }

    public static class HotelImage {

        private String thumbnail;
        @SerializedName("original_image")
        private String originalImage;

        public String getThumbnail() {
            return thumbnail;
        }

        public String getOriginalImage() {
            return originalImage;
        }
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

    public double getLongitude() {
        if (coordinates != null) {
            return coordinates.getLongitude();
        }
        return longitude;
    }

    public double getLatitude() {
        if (coordinates != null) {
            return coordinates.getLatitude();
        }
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getImageUrl() {
        if (hotelImages != null && !hotelImages.isEmpty()) {
            return hotelImages.get(0).getThumbnail();
        }
        return imageUrl;
    }

    public String getOriginalImageUrl() {
        if (hotelImages != null && !hotelImages.isEmpty()) {
            return hotelImages.get(0).getOriginalImage();
        }
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

    public String getPrice() {
        if (ratePerNight != null && ratePerNight.getLowest() != null) {
            return ratePerNight.getLowest();
        }
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}
