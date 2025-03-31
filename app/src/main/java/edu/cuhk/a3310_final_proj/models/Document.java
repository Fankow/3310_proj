package edu.cuhk.a3310_final_proj.models;

import com.google.firebase.firestore.PropertyName;

public class Document {

    private String id;
    private String name;
    private String type;

    @PropertyName("file_url")
    private String fileUrl;

    // Required empty constructor for Firestore
    public Document() {
    }

    // Getters and Setters
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @PropertyName("file_url")
    public String getFileUrl() {
        return fileUrl;
    }

    @PropertyName("file_url")
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
