package edu.cuhk.a3310_final_proj.models;

import com.google.firebase.firestore.PropertyName;
import java.util.Date;

public class Expense {

    private String id;
    private String description;
    private double amount;
    private String currency;
    private Date date;
    private String category;

    @PropertyName("receipt_image_url")
    private String receiptImageUrl;

    private String trip_id;

    public Expense() {
        this.date = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @PropertyName("receipt_image_url")
    public String getReceiptImageUrl() {
        return receiptImageUrl;
    }

    @PropertyName("receipt_image_url")
    public void setReceiptImageUrl(String receiptImageUrl) {
        this.receiptImageUrl = receiptImageUrl;
    }

    @PropertyName("trip_id")
    public String getTripId() {
        return trip_id;
    }

    @PropertyName("trip_id")
    public void setTripId(String tripId) {
        this.trip_id = tripId;
    }
}
