package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SyncPollResponse {

    @SerializedName("since")
    private String since;

    @SerializedName("serverTime")
    private String serverTime;

    @SerializedName("changes")
    private List<BookingChange> changes;

    public String getSince() { return since; }
    public String getServerTime() { return serverTime; }
    public List<BookingChange> getChanges() { return changes; }
}
