package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;
import com.example.androidapp.data.model.AuthResponse;
import com.example.androidapp.data.model.LoginRequest;
import com.example.androidapp.data.model.OtpRequest;
import com.example.androidapp.data.model.OtpVerify;
import com.example.androidapp.data.model.RegisterRequest;
import com.example.androidapp.data.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/register")
    Call<ApiResponse<User>> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/otp/request")
    Call<ApiResponse<Map<String, String>>> requestOtp(@Body OtpRequest request);

    @POST("auth/otp/verify")
    Call<ApiResponse<AuthResponse>> verifyOtp(@Body OtpVerify request);

    @POST("auth/otp/resend")
    Call<ApiResponse<Map<String, String>>> resendOtp(@Body OtpRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Map<String, String>>> logout(@Header("Authorization") String token);

    @GET("auth/me")
    Call<ApiResponse<User>> getMe(@Header("Authorization") String token);
}
