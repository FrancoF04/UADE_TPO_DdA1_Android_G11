package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Schedule {

    @SerializedName("id")
    private final String id;

    @SerializedName("date")
    private final String date;

    @SerializedName("availableSpots")
    private final int availableSpots;

    @SerializedName("totalSpots")
    private final int totalSpots;

    public Schedule(String id, String date, int availableSpots, int totalSpots) {
        this.id = id;
        this.date = date;
        this.availableSpots = availableSpots;
        this.totalSpots = totalSpots;
    }

    public String getId() { return id; }
    public String getDate() { return date; }
    public int getAvailableSpots() { return availableSpots; }
    public int getTotalSpots() { return totalSpots; }
}