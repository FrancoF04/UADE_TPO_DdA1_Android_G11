package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReservationsData {
    @SerializedName("detailedActivities")
    private final List<Reservation> detailedActivities;

    public ReservationsData(List<Reservation> detailedActivities) {
        this.detailedActivities = detailedActivities;
    }

    public List<Reservation> getDetailedActivities() {
        return detailedActivities;
    }
}
