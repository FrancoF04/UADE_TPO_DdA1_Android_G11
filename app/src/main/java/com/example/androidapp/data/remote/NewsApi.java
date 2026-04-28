package com.example.androidapp.data.remote;

import com.example.androidapp.data.model.ApiResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NewsApi {

    @GET("news")
    Call<ApiResponse<List<Object>>> getNews(@Query("page") Integer page, @Query("page_size") Integer pageSize);

    @GET("news/{id}")
    Call<ApiResponse<Object>> getNewsById(@Path("id") String id);
}
