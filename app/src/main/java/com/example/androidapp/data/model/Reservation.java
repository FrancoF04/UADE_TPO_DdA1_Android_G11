package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Reservation {
    @SerializedName("activityId")
    private final String activityId;
    @SerializedName("selectedDate")
    private final String selectedDate;
    @SerializedName("selectedScheduleId")
    private final String selectedScheduleId;
    @SerializedName("quantity")
    private final int quantity;
    @SerializedName("cancellationHours")
    private final int cancellationHours;
    @SerializedName("status")
    private final String status;

    public Reservation(String activityId, String selectedDate, String selectedScheduleId,
                       int quantity, int cancellationHours, String status) {
        this.activityId = activityId;
        this.selectedDate = selectedDate;
        this.selectedScheduleId = selectedScheduleId;
        this.quantity = quantity;
        this.cancellationHours = cancellationHours;
        this.status = status;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public String getSelectedScheduleId() {
        return selectedScheduleId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getCancellationHours() {
        return cancellationHours;
    }

    public String getStatus() {
        return status;
    }
}
