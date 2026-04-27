package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class UserUpdate {

    @SerializedName("email")
    private final String email;

    @SerializedName("phoneNumber")
    private final String phoneNumber;

    @SerializedName("fullName")
    private final String fullName;

    public UserUpdate(String email, String phoneNumber, String fullName) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }
}
