package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class Reservation {
    @SerializedName("id")
    private final String id;
    @SerializedName("activityId")
    private final String activityId;
    
    @SerializedName("activityName")
    private final String activityName;

    @SerializedName(value = "imageUrl", alternate = {"image_url"})
    private final String imageUrl;

    @SerializedName("image")
    private final String image;

    @SerializedName("activity")
    private final ActivitySummary activity;
    @SerializedName("selectedDate")
    private final String selectedDate;
    @SerializedName("selectedScheduleId")
    private final String selectedScheduleId;
    @SerializedName("quantity")
    private final int quantity;
    @SerializedName("cancellationHours")
    private final int cancellationHours;
    @SerializedName("status")
    private final String status;

    public Reservation(String id, String activityId, String activityName, String selectedDate,
                       String selectedScheduleId, int quantity, int cancellationHours, String status) {
        this.id = id;
        this.activityId = activityId;
        this.activityName = activityName;
        this.imageUrl = null;
        this.image = null;
        this.activity = null;
        this.selectedDate = selectedDate;
        this.selectedScheduleId = selectedScheduleId;
        this.quantity = quantity;
        this.cancellationHours = cancellationHours;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public String getSelectedScheduleId() {
        return selectedScheduleId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getCancellationHours() {
        return cancellationHours;
    }

    public String getStatus() {
        return status;
    }

    public String getActivityName() {
        if (activityName != null && !activityName.isEmpty()) {
            return activityName;
        }

        if (activity != null) {
            return activity.getName();
        }

        return null;
    }

    public String getActivityImageUrl() {
        if (activity != null) {
            String nestedImage = activity.getImageUrl();
            if (nestedImage != null && !nestedImage.isEmpty()) {
                return nestedImage;
            }

            String nestedAlternate = activity.getImage();
            if (nestedAlternate != null && !nestedAlternate.isEmpty()) {
                return nestedAlternate;
            }
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        }

        if (image != null && !image.isEmpty()) {
            return image;
        }

        return null;
    }

    public static class ActivitySummary {
        @SerializedName("name")
        private final String name;

        @SerializedName(value = "imageUrl", alternate = {"image_url"})
        private final String imageUrl;

        @SerializedName("image")
        private final String image;

        public ActivitySummary(String name) {
            this.name = name;
            this.imageUrl = null;
            this.image = null;
        }

        public String getName() {
            return name;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getImage() {
            return image;
        }
    }
}
