package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserUpdate;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;

public interface UserApi {

    @GET("users/me")
    Call<ApiResponse<User.UserResponse>> getUser(@Header("Authorization") String token);

    @PUT("users/me")
    Call<ApiResponse<User.UserResponse>> updateUser(@Header("Authorization") String token, @Body UserUpdate user);
}
