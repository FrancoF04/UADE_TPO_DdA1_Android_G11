package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Activity {

    @SerializedName("id")
    private final String id;

    @SerializedName("name")
    private final String name;

    @SerializedName("destination")
    private final String destination;

    @SerializedName("category")
    private final String category;

    @SerializedName("description")
    private final String description;

    @SerializedName("imageUrl")
    private final String imageUrl;

    @SerializedName("galleryUrls")
    private final List<String> galleryUrls;

    @SerializedName("duration")
    private final String duration;

    @SerializedName("price")
    private final double price;

    @SerializedName("currency")
    private final String currency;

    @SerializedName("availableSpots")
    private final int availableSpots;

    @SerializedName("totalSpots")
    private final int totalSpots;

    @SerializedName("date")
    private final List<String> date;

    @SerializedName("meetingPoint")
    private final String meetingPoint;

    @SerializedName("guide")
    private final Guide guide;

    @SerializedName("language")
    private final String language;

    @SerializedName("included")
    private final List<String> included;

    @SerializedName("cancellationPolicy")
    private final String cancellationPolicy;

    @SerializedName("featured")
    private final boolean featured;

    public Activity(String id, String name, String destination, String category,
                    String description, String imageUrl, List<String> galleryUrls,
                    String duration, double price, String currency,
                    int availableSpots, int totalSpots, String date,
                    String meetingPoint, Guide guide, String language,
                    List<String> included, String cancellationPolicy, boolean featured) {
        this.id = id;
        this.name = name;
        this.destination = destination;
        this.category = category;
        this.description = description;
        this.imageUrl = imageUrl;
        this.galleryUrls = galleryUrls;
        this.duration = duration;
        this.price = price;
        this.currency = currency;
        this.availableSpots = availableSpots;
        this.totalSpots = totalSpots;
        this.date = date;
        this.meetingPoint = meetingPoint;
        this.guide = guide;
        this.language = language;
        this.included = included;
        this.cancellationPolicy = cancellationPolicy;
        this.featured = featured;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDestination() { return destination; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getGalleryUrls() { return galleryUrls; }
    public String getDuration() { return duration; }
    public double getPrice() { return price; }
    public String getCurrency() { return currency; }
    public int getAvailableSpots() { return availableSpots; }
    public int getTotalSpots() { return totalSpots; }
    public String getDate() { return date; }
    public String getMeetingPoint() { return meetingPoint; }
    public Guide getGuide() { return guide; }
    public String getLanguage() { return language; }
    public List<String> getIncluded() { return included; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public boolean isFeatured() { return featured; }
}
