package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class OfflineBundle {
    @SerializedName("bookings")
    private List<Reservation> bookings;
    @SerializedName("vouchers")
    private List<VoucherItem> vouchers;
    @SerializedName("activities")
    private List<Activity> activities;

    public List<Reservation> getBookings() { return bookings; }
    public List<VoucherItem> getVouchers() { return vouchers; }
    public List<Activity> getActivities() { return activities; }
}
