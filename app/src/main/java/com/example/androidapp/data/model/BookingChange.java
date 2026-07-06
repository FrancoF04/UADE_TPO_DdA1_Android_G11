package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class BookingChange {

    @SerializedName("bookingId")
    private String bookingId;

    @SerializedName("activityId")
    private String activityId;

    @SerializedName("status")
    private String status;

    @SerializedName("selectedDate")
    private String selectedDate;

    @SerializedName("selectedScheduleId")
    private String selectedScheduleId;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("changeType")
    private String changeType;

    public String getBookingId() { return bookingId; }
    public String getActivityId() { return activityId; }
    public String getStatus() { return status; }
    public String getSelectedDate() { return selectedDate; }
    public String getSelectedScheduleId() { return selectedScheduleId; }
    public String getUpdatedAt() { return updatedAt; }
    public String getChangeType() { return changeType; }
}
