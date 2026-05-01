package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.FavoriteRequest;
import com.example.androidapp.data.model.FavoriteResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Body;

public interface FavoritesApi {

    @GET("favorites")
    Call<ApiResponse<List<Activity>>> getFavorites();

    @POST("favorites")
    Call<ApiResponse<FavoriteResponse>> addFavorite(@Body FavoriteRequest request);

    @DELETE("favorites/{activityId}")
    Call<ApiResponse<Void>> removeFavorite(@Path("activityId") String activityId);
}
