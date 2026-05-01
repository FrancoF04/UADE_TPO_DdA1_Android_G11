package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("token")
    private final String token;

    @SerializedName("user")
    private final User user;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("expiresAt")
    private String expiresAt;

    @SerializedName("refreshExpiresAt")
    private String refreshExpiresAt;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public User getUser() { return user; }
    public String getRefreshToken() { return refreshToken; }
    public String getExpiresAt() { return expiresAt; }
    public String getRefreshExpiresAt() { return refreshExpiresAt; }
}
