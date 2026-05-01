package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.FavoriteRequest;
import com.example.androidapp.data.model.FavoriteResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Body;
import retrofit2.http.Query;

public interface FavoritesApi {

    @Headers("Cache-Control: no-cache")
    @GET("favorites")
    Call<ApiResponse<List<Activity>>> getFavorites(@Query("t") long timestamp);

    @POST("favorites")
    Call<ApiResponse<FavoriteResponse>> addFavorite(@Body FavoriteRequest request);

    @DELETE("favorites/{activityId}")
    Call<ApiResponse<Void>> removeFavorite(@Path("activityId") String activityId);
}
