package com.example.androidapp;

import android.app.Application;

import com.example.androidapp.util.NotificationHelper;

import dagger.hilt.android.HiltAndroidApp;
import org.osmdroid.config.Configuration;

@HiltAndroidApp
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Configuration.getInstance().setUserAgentValue(getPackageName());
        NotificationHelper.createChannels(this);
    }
}
