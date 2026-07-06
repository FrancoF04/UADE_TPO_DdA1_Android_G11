package com.example.androidapp.di;

import static com.example.androidapp.BuildConfig.API_BASE_URL;

import android.content.Context;

import com.example.androidapp.data.local.NewsCache;
import com.example.androidapp.data.local.OfflineBookingCache;
import com.example.androidapp.data.remote.ActivityApi;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.remote.BookingsApi;
import com.example.androidapp.data.remote.UserApi;
import com.example.androidapp.data.remote.RatingsApi;
import com.example.androidapp.data.remote.FavoritesApi;
import com.example.androidapp.data.remote.NewsApi;
import com.example.androidapp.data.remote.NotificationsApi;
import com.example.androidapp.data.remote.SyncApi;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module @InstallIn(SingletonComponent.class)
public class NetworkModule {

    @Provides
    @Singleton
    public OkHttpClient provideOkHttp(AuthRefreshInterceptor authInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
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

    @Provides
    @Singleton
    public NewsCache provideNewsCache(@ApplicationContext Context context, Gson gson) {
        return new NewsCache(context, gson);
    }

    @Provides
    @Singleton
    public BookingsApi provideBookingsApiService(Retrofit retrofit) {
        return retrofit.create(BookingsApi.class);
    }

    @Provides
    @Singleton
    public OfflineBookingCache provideOfflineBookingCache(@ApplicationContext Context context, Gson gson) {
        return new OfflineBookingCache(context, gson);
    }

    // Cliente/Retrofit dedicados al long polling de notificaciones: necesitan un read timeout
    // largo (el server retiene la respuesta ~25s) sin afectar el timeout corto del resto de las Api.
    @Provides
    @Singleton
    @LongPolling
    public OkHttpClient providePollingOkHttp(AuthRefreshInterceptor authInterceptor) {
        return new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .readTimeout(35, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    @LongPolling
    public Retrofit providePollingRetrofit(@LongPolling OkHttpClient okHttpClient, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public NotificationsApi provideNotificationsApiService(@LongPolling Retrofit retrofit) {
        return retrofit.create(NotificationsApi.class);
    }

    @Provides
    @Singleton
    public SyncApi provideSyncApiService(@LongPolling Retrofit retrofit) {
        return retrofit.create(SyncApi.class);
    }
}
