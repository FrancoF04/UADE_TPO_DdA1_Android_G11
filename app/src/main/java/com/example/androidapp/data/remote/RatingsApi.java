package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.RatingData;
import com.example.androidapp.data.model.RatingRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RatingsApi {

    @POST("ratings")
    Call<ApiResponse<RatingData>> submitRating(@Body RatingRequest request);

    @GET("ratings/{bookingId}")
    Call<ApiResponse<RatingData>> getRating(@Path("bookingId") String bookingId);
}
