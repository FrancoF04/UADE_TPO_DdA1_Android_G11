package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.User;
import com.example.androidapp.data.model.UserPreferences;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.PUT;

public interface UserApi {

    @PUT("users/preferences")
    Call<ApiResponse<User>> updatePreferences(
            @Header("Authorization") String token,
            @Body UserPreferences preferences);
}
