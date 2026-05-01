package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class FavoriteRequest {
    @SerializedName("activityId")
    private final String activityId;

    public FavoriteRequest(String activityId) {
        this.activityId = activityId;
    }

    public String getActivityId() {
        return activityId;
    }
}
