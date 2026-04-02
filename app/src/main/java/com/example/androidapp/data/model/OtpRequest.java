package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class OtpRequest {

    @SerializedName("email")
    private final String email;

    public OtpRequest(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
}
