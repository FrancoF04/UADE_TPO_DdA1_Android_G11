package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.Rating;
import com.example.androidapp.data.model.RatingRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface RatingsApi {

    @POST("ratings")
    Call<ApiResponse<Rating>> submitRating(@Body RatingRequest request);

    @GET("ratings/{bookingId}")
    Call<ApiResponse<Rating>> getRating(@Path("bookingId") String bookingId);
}
