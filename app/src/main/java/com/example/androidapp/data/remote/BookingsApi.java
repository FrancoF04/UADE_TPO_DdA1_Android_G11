package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.OfflineBundle;

import retrofit2.Call;
import retrofit2.http.GET;

public interface BookingsApi {

    @GET("bookings/offline-bundle")
    Call<ApiResponse<OfflineBundle>> getOfflineBundle();
}
