package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("email")
    private final String email;

    @SerializedName("username")
    private final String username;

    @SerializedName("password")
    private final String password;

    @SerializedName("fullName")
    private final String fullName;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    public RegisterRequest(String email, String username, String password, String fullName, String phoneNumber) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
}
