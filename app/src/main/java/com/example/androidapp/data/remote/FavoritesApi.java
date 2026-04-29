package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Body;

public interface FavoritesApi {

    @GET("/favorites")
    Call<ApiResponse<Object>> getFavorites();

    @POST("/favorites")
    Call<ApiResponse<Object>> addFavorite(@Body Map<String, Object> body);

    @DELETE("/favorites/{activityId}")
    Call<ApiResponse<Object>> removeFavorite(@Path("activityId") String activityId);
}
