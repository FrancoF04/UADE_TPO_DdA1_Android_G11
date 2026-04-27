package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class UserPreferencesRequest {

    @SerializedName("categories")
    private final List<String> categories;

    @SerializedName("destinations")
    private final List<String> destinations;

    public UserPreferencesRequest(List<String> categories) {
        this.categories = categories;
        this.destinations = new ArrayList<>(); // Por ahora mandamos destinos vacío
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getDestinations() {
        return destinations;
    }
}
