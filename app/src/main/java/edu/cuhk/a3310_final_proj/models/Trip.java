package edu.cuhk.a3310_final_proj.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Trip {

    @DocumentId
    private String id;

    private String name;
    private String destination;

    @PropertyName("start_date")
    private Date startDate;

    @PropertyName("end_date")
    private Date endDate;

    private double budget;
    private String currency = "USD";
    private String notes;

    @PropertyName("flight_number")
    private String flightNumber;

    @PropertyName("owner_id")
    private String ownerId;

    private List<Location> locations = new ArrayList<>();
    private List<Expense> expenses = new ArrayList<>();

    @PropertyName("image_url")
    private String imageUrl;

    @PropertyName("created_at")
    private Date createdAt;

    @PropertyName("updated_at")
    private Date updatedAt;

    private List<Document> documents = new ArrayList<>();

    // Required empty constructor for Firestore
    public Trip() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Constructor with basic fields
    public Trip(String name, String destination, Date startDate, Date endDate) {
        this.name = name;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = new Date();
        this.updatedAt = new Date();
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

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @PropertyName("start_date")
    public Date getStartDate() {
        return startDate;
    }

    @PropertyName("start_date")
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @PropertyName("end_date")
    public Date getEndDate() {
        return endDate;
    }

    @PropertyName("end_date")
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @PropertyName("flight_number")
    public String getFlightNumber() {
        return flightNumber;
    }

    @PropertyName("flight_number")
    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    @PropertyName("owner_id")
    public String getOwnerId() {
        return ownerId;
    }

    @PropertyName("owner_id")
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    @PropertyName("image_url")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("image_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("created_at")
    public Date getCreatedAt() {
        return createdAt;
    }

    @PropertyName("created_at")
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updated_at")
    public Date getUpdatedAt() {
        return updatedAt;
    }

    @PropertyName("updated_at")
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }

    // Helper method to add a location
    public void addLocation(Location location) {
        if (this.locations == null) {
            this.locations = new ArrayList<>();
        }
        this.locations.add(location);
    }

    // Helper method to add an expense
    public void addExpense(Expense expense) {
        if (this.expenses == null) {
            this.expenses = new ArrayList<>();
        }
        this.expenses.add(expense);
    }

    // Helper method to add a document
    public void addDocument(Document document) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
        this.documents.add(document);
    }
}
