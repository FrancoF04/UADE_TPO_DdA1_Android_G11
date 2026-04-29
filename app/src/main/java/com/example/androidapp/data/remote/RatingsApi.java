package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RatingsApi {

    @POST("ratings")
    Call<ApiResponse<Object>> submitRating(@Body Map<String, Object> ratingBody);

    @GET("ratings/{bookingId}")
    Call<ApiResponse<Object>> getRating(@Path("bookingId") String bookingId);
}
