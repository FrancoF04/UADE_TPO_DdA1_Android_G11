package com.example.androidapp.di;

import static com.example.androidapp.BuildConfig.API_BASE_URL;

import android.content.Context;
import android.util.Log;

import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.remote.UserApi;
import com.example.androidapp.data.remote.RatingsApi;
import com.example.androidapp.data.remote.FavoritesApi;
import com.example.androidapp.data.remote.NewsApi;
import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module @InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String TAG = "NetworkModule";

    //agregado un provideTokenManager porque no podia inyectarlo al provideOkHttp sin el
    @Provides
    @Singleton
    public TokenManager provideTokenManager(@ApplicationContext Context context) {
        return TokenManager.getInstance(context);
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttp(TokenManager tokenManager) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    String token = tokenManager.getToken();
                    Request request = chain.request();
                    if (token != null) {
                        request = request.newBuilder()
                                .addHeader("Authorization", "Bearer " + token)
                                .build();
                        Log.d(TAG, "Authorization header agregado: Bearer " + token);
                    } else {
                        Log.d(TAG, "Sin token — request sin Authorization header");
                    }
                    return chain.proceed(request);
                })
                .build();
    }

    @Provides
    @Singleton
    public Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
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

    @Provides
    @Singleton
    public RatingsApi provideRatingsApiService(Retrofit retrofit) {
        return retrofit.create(RatingsApi.class);
    }

    @Provides
    @Singleton
    public FavoritesApi provideFavoritesApiService(Retrofit retrofit) {
        return retrofit.create(FavoritesApi.class);
    }

    @Provides
    @Singleton
    public NewsApi provideNewsApiService(Retrofit retrofit) {
        return retrofit.create(NewsApi.class);
    }
}
