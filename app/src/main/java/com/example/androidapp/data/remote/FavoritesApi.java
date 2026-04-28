package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Body;
import retrofit2.http.Header;

public interface FavoritesApi {

    @GET("favorites")
    Call<ApiResponse<Object>> getFavorites(@Header("Authorization") String token);

    @POST("favorites")
    Call<ApiResponse<Object>> addFavorite(@Header("Authorization") String token, @Body Map<String, Object> body);

    @DELETE("favorites/{activityId}")
    Call<ApiResponse<Object>> removeFavorite(@Header("Authorization") String token, @Path("activityId") String activityId);
}
