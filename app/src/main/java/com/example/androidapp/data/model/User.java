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

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    public User(String id, String email, String username, String fullName, String phoneNumber) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }

    // Clase para mapear el envoltorio {"data": {"user": {...}}}
    public static class UserResponse {
        @SerializedName("user")
        private final User user;

        public UserResponse(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }
    }
}
