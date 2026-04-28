package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class ReservationRequest {
    @SerializedName("activityId")
    private final String activityId;
    @SerializedName("selectedDate")
    private final String selectedDate;
    @SerializedName("quantity")
    private final int quantity;

    public ReservationRequest(String activityId, String selectedDate, int quantity) {
        this.activityId = activityId;
        this.selectedDate = selectedDate;
        this.quantity = quantity;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public int getQuantity() {
        return quantity;
    }
}
