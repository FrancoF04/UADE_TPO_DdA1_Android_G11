package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserPreferences {

    @SerializedName("categories")
    private final List<String> categories;

    @SerializedName("destinations")
    private final List<String> destinations;

    public UserPreferences(List<String> categories, List<String> destinations) {
        this.categories = categories;
        this.destinations = destinations;
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getDestinations() {
        return destinations;
    }
}
