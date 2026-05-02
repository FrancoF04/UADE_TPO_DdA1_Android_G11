package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class HistorialItem {

    @SerializedName("bookingId")
    private String bookingId;

    @SerializedName("activityId")
    private String activityId;

    @SerializedName("activityName")
    private String activityName;

    @SerializedName("destination")
    private String destination;

    @SerializedName("guide")
    private Guide guide;

    @SerializedName("duration")
    private String duration;

    @SerializedName("date")
    private String date;

    @SerializedName("imageUrl")
    private String imageUrl;

    public String getBookingId() { return bookingId; }
    public String getActivityId() { return activityId; }
    public String getActivityName() { return activityName; }
    public String getDestination() { return destination != null ? destination : ""; }
    public String getDuration() { return duration != null ? duration : ""; }
    public String getSelectedDate() { return date; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String url) { this.imageUrl = url; }

    public String getGuideName() {
        return guide != null && guide.getName() != null ? guide.getName() : "";
    }
}
