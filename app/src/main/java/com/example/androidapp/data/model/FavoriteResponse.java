package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class FavoriteResponse {
    @SerializedName("favorite")
    private final Activity favorite;

    public FavoriteResponse(Activity favorite) {
        this.favorite = favorite;
    }

    public Activity getFavorite() {
        return favorite;
    }
}
