package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FilterOptions {

    @SerializedName("destinations")
    private final List<String> destinations;

    @SerializedName("categories")
    private final List<String> categories;

    public FilterOptions(List<String> destinations, List<String> categories) {
        this.destinations = destinations;
        this.categories = categories;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    public List<String> getCategories() {
        return categories;
    }
}
