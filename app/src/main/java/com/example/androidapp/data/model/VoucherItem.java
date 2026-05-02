package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class VoucherItem {
    @SerializedName("bookingId")
    private String bookingId;
    @SerializedName("voucherCode")
    private String voucherCode;

    public String getBookingId() { return bookingId; }
    public String getVoucherCode() { return voucherCode; }
}
