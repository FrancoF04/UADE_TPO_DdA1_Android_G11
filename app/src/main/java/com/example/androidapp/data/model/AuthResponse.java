package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("token")
    private final String token;

    @SerializedName("user")
    private final User user;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }
}
