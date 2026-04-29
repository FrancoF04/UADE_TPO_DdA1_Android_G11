package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class HistorialItem {

    @SerializedName("id")
    private String id;

    @SerializedName("activityId")
    private String activityId;

    @SerializedName("activityName")
    private String activityName;

    @SerializedName("selectedDate")
    private String selectedDate;

    @SerializedName("status")
    private String status;

    @SerializedName("activity")
    private Activity activity;

    public String getId() { return id; }
    public String getActivityId() { return activityId; }
    public String getActivityName() { return activityName; }
    public String getSelectedDate() { return selectedDate; }
    public String getStatus() { return status; }
    public Activity getActivity() { return activity; }

    public String getDestination() {
        return activity != null ? activity.getDestination() : "";
    }

    public String getGuideName() {
        return (activity != null && activity.getGuide() != null)
                ? activity.getGuide().getName()
                : "";
    }

    public String getDuration() {
        return activity != null ? activity.getDuration() : "";
    }
}
