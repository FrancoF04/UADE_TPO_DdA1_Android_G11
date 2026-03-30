package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Guide {

    @SerializedName("name")
    private final String name;

    @SerializedName("rating")
    private final double rating;

    public Guide(String name, double rating) {
        this.name = name;
        this.rating = rating;
    }

    public String getName() {
        return name;
    }

    public double getRating() {
        return rating;
    }
}
