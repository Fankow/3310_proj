package edu.cuhk.a3310_final_proj.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Airport implements Serializable {

    private String name;
    private String id;

    private String time;
    private String terminal;
    private String gate;

    // Default constructor
    public Airport() {
    }

    // Constructor with basic fields
    public Airport(String id, String name, String time) {
        this.id = id;
        this.name = name;
        this.time = time;
    }

    @SerializedName("name")
    public String getName() {
        return name;
    }

    @SerializedName("name")
    public void setName(String name) {
        this.name = name;
    }

    @SerializedName("id")
    public String getId() {
        return id;
    }

    @SerializedName("id")
    public void setId(String id) {
        this.id = id;
    }

    @SerializedName("time")
    public String getTime() {
        return time;
    }

    @SerializedName("time")
    public void setTime(String time) {
        this.time = time;
    }

    @SerializedName("terminal")
    public String getTerminal() {
        return terminal;
    }

    @SerializedName("terminal")
    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    @SerializedName("gate")
    public String getGate() {
        return gate;
    }

    @SerializedName("gate")
    public void setGate(String gate) {
        this.gate = gate;
    }

    @Override
    public String toString() {
        return "Airport{"
                + "id='" + id + '\''
                + ", name='" + name + '\''
                + ", time='" + time + '\''
                + '}';
    }
}
