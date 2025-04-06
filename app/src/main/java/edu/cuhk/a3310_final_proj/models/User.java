package edu.cuhk.a3310_final_proj.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;

public class User {

    @DocumentId
    private String id;
    private String email;
    private String displayName;
    private String photoUrl;
    private List<String> monitoredCurrencies;
    @PropertyName("default_currency")
    private String defaultCurrency = "USD";

    @PropertyName("preferred_language")
    private String preferredLanguage = "en";

    @PropertyName("dark_mode")
    private boolean darkMode = false;

    // Required empty constructor for Firestore
    public User() {
    }

    public User(String email, String displayName) {
        this.email = email;
        this.displayName = displayName;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    @PropertyName("default_currency")
    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    @PropertyName("default_currency")
    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    @PropertyName("dark_mode")
    public boolean isDarkMode() {
        return darkMode;
    }

    @PropertyName("dark_mode")
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    @PropertyName("monitored_currencies")
    public List<String> getMonitoredCurrencies() {
        if (monitoredCurrencies == null) {
            monitoredCurrencies = new ArrayList<>();
        }
        return monitoredCurrencies;
    }

    @PropertyName("monitored_currencies")
    public void setMonitoredCurrencies(List<String> monitoredCurrencies) {
        this.monitoredCurrencies = monitoredCurrencies;
    }
}
