package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class RatingRequest {

    @SerializedName(value = "bookingId", alternate = "booking_id")
    private final String bookingId;

    @SerializedName(value = "activityRating", alternate = "activity_rating")
    private final int activityRating;

    @SerializedName(value = "guideRating", alternate = "guide_rating")
    private final int guideRating;

    @SerializedName("comment")
    private final String comment;

    public RatingRequest(String bookingId, int activityRating, int guideRating, String comment) {
        this.bookingId = bookingId;
        this.activityRating = activityRating;
        this.guideRating = guideRating;
        this.comment = comment;
    }
}
