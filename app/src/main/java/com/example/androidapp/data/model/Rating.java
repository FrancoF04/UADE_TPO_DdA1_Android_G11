package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Rating {

    @SerializedName("id")
    private String id;

    @SerializedName("bookingId")
    private String bookingId;

    @SerializedName("userId")
    private String userId;

    @SerializedName(value = "activityRating", alternate = "activity_rating")
    private int activityRating;

    @SerializedName(value = "guideRating", alternate = "guide_rating")
    private int guideRating;

    @SerializedName("comment")
    private String comment;

    @SerializedName(value = "createdAt", alternate = "created_at")
    private String createdAt;

    public String getId() { return id; }
    public String getBookingId() { return bookingId; }
    public int getActivityRating() { return activityRating; }
    public int getGuideRating() { return guideRating; }
    public String getComment() { return comment != null ? comment : ""; }
    public String getCreatedAt() { return createdAt; }
}
