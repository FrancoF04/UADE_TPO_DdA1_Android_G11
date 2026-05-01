package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.HistorialItem;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface ActivityApi {

    @GET("activities")
    Call<ApiResponse<List<Activity>>> getActivities(
            @Query("page") int page,
            @Query("limit") int limit);

    @GET("activities")
    Call<ApiResponse<List<Activity>>> getActivitiesFiltered(
            @Query("page") int page,
            @Query("limit") int limit,
            @QueryMap Map<String, String> filters);

    @GET("activities")
    Call<ApiResponse<List<Activity>>> searchActivities(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("destination") String destination,
            @Query("category") String category);

    @GET("activities/{id}")
    Call<ApiResponse<Activity>> getActivityById(@Path("id") String id);

    @GET("activities/history")
    Call<ApiResponse<List<HistorialItem>>> getHistory(
            @Query("page") int page,
            @Query("limit") int limit);
    @GET("activities/recommended")
    Call<ApiResponse<List<Activity>>> getRecommended();

    @GET("activities/featured")
    Call<ApiResponse<List<Activity>>> getFeatured();

}
