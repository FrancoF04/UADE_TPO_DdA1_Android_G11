package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NotificationPollResponse {

    @SerializedName("events")
    private List<NotificationItem> events;

    public List<NotificationItem> getEvents() { return events; }
}
