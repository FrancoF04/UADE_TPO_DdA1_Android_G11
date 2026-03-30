package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class OtpVerify {

    @SerializedName("email")
    private final String email;

    @SerializedName("code")
    private final String code;

    public OtpVerify(String email, String code) {
        this.email = email;
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }
}
