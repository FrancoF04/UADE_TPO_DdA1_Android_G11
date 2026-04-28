package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.ReservationRequest;
import com.example.androidapp.data.model.Reservation;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserUpdate;
import com.example.androidapp.data.model.UserPreferencesRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface UserApi {

    @GET("users/me")
    Call<ApiResponse<User.UserResponse>> getUser(@Header("Authorization") String token);

    @POST("users/activities")
    Call<ApiResponse<Reservation>> createReservation(@Header("Authorization") String token, @Body ReservationRequest reservation);

    @PUT("users/me")
    Call<ApiResponse<User.UserResponse>> updateUser(@Header("Authorization") String token, @Body UserUpdate user);

    @PUT("users/preferences")
    Call<ApiResponse<User.UserResponse>> updatePreferences(@Header("Authorization") String token, @Body UserPreferencesRequest preferences);
}
