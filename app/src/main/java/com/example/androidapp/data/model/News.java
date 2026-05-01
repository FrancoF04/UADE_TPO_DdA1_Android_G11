package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class News {

    @SerializedName("id")
    private String id;

    @SerializedName("image")
    private String image;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("activityId")
    private String activityId;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }
    public String getImage() { return image; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getActivityId() { return activityId; }
    public String getCreatedAt() { return createdAt; }

    public boolean hasRelatedActivity() {
        return activityId != null && !activityId.isEmpty();
    }
}
