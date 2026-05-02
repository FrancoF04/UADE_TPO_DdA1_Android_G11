package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.BookingsSummary;
import com.example.androidapp.data.model.ReservationRequest;
import com.example.androidapp.data.model.Reservation;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserUpdate;
import com.example.androidapp.data.model.UserPreferencesRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface UserApi {

    // Perfil
    @GET("profile")
    Call<ApiResponse<User.UserResponse>> getUser();

    @PUT("users/me")
    Call<ApiResponse<User.UserResponse>> updateUser(@Body UserUpdate user);

    @GET("profile/preferences")
    Call<ApiResponse<User.UserResponse>> getPreferences();

    @PUT("profile/preferences")
    Call<ApiResponse<User.UserResponse>> updatePreferences(@Body UserPreferencesRequest preferences);

    @GET("profile/bookings-summary")
    Call<ApiResponse<BookingsSummary>> getBookingsSummary();

    @GET("users/reservations")
    Call<ApiResponse<List<Reservation>>> getReservations();


    @POST("users/reservations")
    Call<ApiResponse<Object>> createReservation(@Body ReservationRequest reservation);

    @DELETE("users/reservations/{id}")
    Call<ApiResponse<Object>> cancelReservation(@Path("id") String id);

    @POST("users/reservations/{id}/cancel")
    Call<ApiResponse<Object>> cancelReservationPost(@Path("id") String id);
    
}
