package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class News {

    @SerializedName("id")
    private String id;

    @SerializedName("category")
    private String category;

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
    public String getCategory() { return category; }
    public String getImage() { return image; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getActivityId() { return activityId; }
    public String getCreatedAt() { return createdAt; }

    public boolean hasRelatedActivity() {
        return activityId != null && !activityId.isEmpty();
    }

    public String categoryLabel() {
        if (category == null) return "Noticia";
        switch (category) {
            case "descuento": return "Descuento";
            case "promocion": return "Promoción";
            case "nuevo_destino": return "Nuevo destino";
            case "noticia":
            default: return "Noticia";
        }
    }
}
