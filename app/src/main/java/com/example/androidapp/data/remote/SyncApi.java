package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.SyncPollResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SyncApi {

    @GET("bookings/sync/poll")
    Call<ApiResponse<SyncPollResponse>> syncPoll(@Query("since") String since);
}
