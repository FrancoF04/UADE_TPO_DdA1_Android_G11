package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {

    @SerializedName("type")
    private String type;

    @SerializedName("bookingId")
    private String bookingId;

    @SerializedName("activityId")
    private String activityId;

    @SerializedName("activityName")
    private String activityName;

    @SerializedName("selectedDate")
    private String selectedDate;

    @SerializedName("voucherCode")
    private String voucherCode;

    public String getType() { return type; }
    public String getBookingId() { return bookingId; }
    public String getActivityId() { return activityId; }
    public String getActivityName() { return activityName; }
    public String getSelectedDate() { return selectedDate; }
    public String getVoucherCode() { return voucherCode; }
}
