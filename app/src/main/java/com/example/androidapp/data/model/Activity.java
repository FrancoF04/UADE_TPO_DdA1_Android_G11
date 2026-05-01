package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;

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

    @SerializedName("schedules")
    private final List<Schedule> schedules;

    @SerializedName(value = "date", alternate = {"dates"})
    @JsonAdapter(StringListOrStringAdapter.class)
    private final List<String> date;

    @SerializedName("meetingPoint")
    @JsonAdapter(MeetingPointAdapter.class)
    private final MeetingPoint meetingPoint;

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

    // Campos adicionales para favoritos (pueden ser nulos)
    @SerializedName("favoriteCreatedAt")
    private final String favoriteCreatedAt;

    @SerializedName("priceAtFavorite")
    private final Double priceAtFavorite;

    @SerializedName("spotsAtFavorite")
    private final Integer spotsAtFavorite;

    @SerializedName("priceChanged")
    private Boolean priceChanged;

    @SerializedName("spotsChanged")
    private Boolean spotsChanged;

    public Activity(String id, String name, String destination, String category,
                    String description, String imageUrl, List<String> galleryUrls,
                    String duration, double price, String currency,
                    int availableSpots, int totalSpots, List<Schedule> schedules, List<String> date,
                    MeetingPoint meetingPoint, Guide guide, String language,
                    List<String> included, String cancellationPolicy, boolean featured) {
        this(id, name, destination, category, description, imageUrl, galleryUrls, duration, price,
                currency, availableSpots, totalSpots, schedules, date, meetingPoint, guide, language,
                included, cancellationPolicy, featured, null, null, null, null, null);
    }

    public Activity(String id, String name, String destination, String category,
                    String description, String imageUrl, List<String> galleryUrls,
                    String duration, double price, String currency,
                    int availableSpots, int totalSpots, List<Schedule> schedules, List<String> date,
                    MeetingPoint meetingPoint, Guide guide, String language,
                    List<String> included, String cancellationPolicy, boolean featured,
                    String favoriteCreatedAt, Double priceAtFavorite, Integer spotsAtFavorite,
                    Boolean priceChanged, Boolean spotsChanged) {
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
        this.schedules = schedules;
        this.date = date;
        this.meetingPoint = meetingPoint;
        this.guide = guide;
        this.language = language;
        this.included = included;
        this.cancellationPolicy = cancellationPolicy;
        this.featured = featured;
        this.favoriteCreatedAt = favoriteCreatedAt;
        this.priceAtFavorite = priceAtFavorite;
        this.spotsAtFavorite = spotsAtFavorite;
        this.priceChanged = priceChanged;
        this.spotsChanged = spotsChanged;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDestination() { return destination; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getGalleryUrls() { return galleryUrls; }
    public String getCoverUrl() {
        if (imageUrl != null && !imageUrl.isEmpty()) return imageUrl;
        if (galleryUrls != null && !galleryUrls.isEmpty()) return galleryUrls.get(0);
        return null;
    }
    public String getDuration() { return duration; }
    public double getPrice() { return price; }
    public String getCurrency() { return currency; }
    public int getAvailableSpots() { return availableSpots; }
    public int getTotalSpots() { return totalSpots; }
    public List<Schedule> getSchedules() { return schedules; }
    public List<String> getDate() { return date; }
    public MeetingPoint getMeetingPoint() { return meetingPoint; }
    public Guide getGuide() { return guide; }
    public String getLanguage() { return language; }
    public List<String> getIncluded() { return included; }
    public String getCancellationPolicy() { return cancellationPolicy; }
    public boolean isFeatured() { return featured; }

    public String getFavoriteCreatedAt() { return favoriteCreatedAt; }
    public Double getPriceAtFavorite() { return priceAtFavorite; }
    public Integer getSpotsAtFavorite() { return spotsAtFavorite; }
    public Boolean getPriceChanged() { return priceChanged != null && priceChanged; }
    public Boolean getSpotsChanged() { return spotsChanged != null && spotsChanged; }

    public void setPriceChanged(Boolean priceChanged) {
        this.priceChanged = priceChanged;
    }

    public void setSpotsChanged(Boolean spotsChanged) {
        this.spotsChanged = spotsChanged;
    }
}
