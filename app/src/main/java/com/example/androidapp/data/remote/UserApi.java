package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.ReservationRequest;
import com.example.androidapp.data.model.Reservation;
import com.example.androidapp.data.model.ReservationsData;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserUpdate;
import com.example.androidapp.data.model.UserPreferencesRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;

public interface UserApi {

    // Perfil
    @GET("profile")
    Call<ApiResponse<User.UserResponse>> getUser(@Header("Authorization") String token);
    @GET("Users/activities")
    Call<ApiResponse<ReservationsData>> getReservations(@Header("Authorization") String token);

    @PATCH("profile")
    Call<ApiResponse<User.UserResponse>> updateUser(@Header("Authorization") String token, @Body UserUpdate user);

    @GET("profile/preferences")
    Call<ApiResponse<User.UserResponse>> getPreferences(@Header("Authorization") String token);

    @PUT("profile/preferences")
    Call<ApiResponse<User.UserResponse>> updatePreferences(@Header("Authorization") String token, @Body UserPreferencesRequest preferences);

    @GET("profile/bookings-summary")
    Call<ApiResponse<Object>> getBookingsSummary(@Header("Authorization") String token);

    @GET("users/reservations")
    Call<ApiResponse<ReservationsData>> getReservations(@Header("Authorization") String token);

    @POST("users/reservations")
    Call<ApiResponse<Reservation>> createReservation(@Header("Authorization") String token, @Body ReservationRequest reservation);

    // Cancelar reserva: DELETE y alias POST cancel
    @DELETE("users/reservations/{id}")
    Call<ApiResponse<Object>> cancelReservation(@Header("Authorization") String token, @Path("id") String id);

    @POST("users/reservations/{id}/cancel")
    Call<ApiResponse<Object>> cancelReservationPost(@Header("Authorization") String token, @Path("id") String id);
