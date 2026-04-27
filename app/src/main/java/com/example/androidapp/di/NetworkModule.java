package com.example.androidapp.di;

import static com.example.androidapp.BuildConfig.API_BASE_URL;

import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.remote.UserApi;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module @InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    //todas la interfaces api que tenemos
    @Provides
    @Singleton
    public UserApi provideUserApiService(Retrofit retrofit) {
        return retrofit.create(UserApi.class);
    }

    @Provides
    @Singleton
    public ActivityApi provideActivityApiService(Retrofit retrofit) {
        return retrofit.create(ActivityApi.class);
    }

    @Provides
    @Singleton
    public AuthApi provideAuthApiService(Retrofit retrofit) {
        return retrofit.create(AuthApi.class);
    }
}
