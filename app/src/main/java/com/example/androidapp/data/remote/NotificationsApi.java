package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.NotificationItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface NotificationsApi {

    @GET("notifications/poll")
    Call<ApiResponse<List<NotificationItem>>> poll();
}
