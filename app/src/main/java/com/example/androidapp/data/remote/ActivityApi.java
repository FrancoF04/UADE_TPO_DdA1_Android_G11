package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.FilterOptions;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ActivityApi {

    @GET("activities")
    Call<ApiResponse<List<Activity>>> getActivities(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("destination") String destination,
            @Query("category") String category,
            @Query("date") String date,
            @Query("priceMin") Double priceMin,
            @Query("priceMax") Double priceMax);

    @GET("activities/featured")
    Call<ApiResponse<List<Activity>>> getFeatured(@Query("limit") int limit);

    @GET("activities/recommended")
    Call<ApiResponse<List<Activity>>> getRecommended(@Header("Authorization") String token);

    @GET("activities/{id}")
    Call<ApiResponse<Activity>> getActivityById(@Path("id") String id);

    @GET("activities/filters")
    Call<ApiResponse<FilterOptions>> getFilters();
}
