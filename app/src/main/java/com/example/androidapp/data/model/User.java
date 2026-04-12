package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("id")
    private final String id;

    @SerializedName("email")
    private final String email;

    @SerializedName("username")
    private final String username;

    @SerializedName("fullName")
    private final String fullName;

    public User(String id, String email, String username, String fullName) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
}
