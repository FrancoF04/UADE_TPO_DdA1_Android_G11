package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class MeetingPoint {

    @SerializedName("latitude")
    private final double latitude;

    @SerializedName("longitude")
    private final double longitude;

    @SerializedName("address")
    private final String address;

    public MeetingPoint(double latitude, double longitude, String address) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }

    public String toDisplayString() {
        if (address != null && !address.isEmpty()) {
            return address;
        }

        return latitude + ", " + longitude;
    }
}