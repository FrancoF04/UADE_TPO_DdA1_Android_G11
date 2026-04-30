package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class RatingData {

    @SerializedName("rating")
    private Rating rating;

    public Rating getRating() { return rating; }
}
