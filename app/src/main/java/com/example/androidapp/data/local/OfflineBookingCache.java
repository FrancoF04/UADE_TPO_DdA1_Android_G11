package com.example.androidapp.data.local;

import android.content.Context;
import android.util.Log;

import com.example.androidapp.data.model.Activity;
import com.example.androidapp.data.model.OfflineBundle;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class OfflineBookingCache {

    private static final String TAG = "OfflineBookingCache";
    private static final String FILE_NAME = "offline_bookings.json";

    private final Context context;
    private final Gson gson;

    @Inject
    public OfflineBookingCache(@ApplicationContext Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }

    public void save(OfflineBundle bundle) {
        String json = gson.toJson(bundle);
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "save failed", e);
        }
    }

    public OfflineBundle read() {
        File f = new File(context.getFilesDir(), FILE_NAME);
        if (!f.exists()) return null;
        try (FileInputStream fis = context.openFileInput(FILE_NAME)) {
            int size = (int) f.length();
            byte[] bytes = new byte[size];
            int read = 0;
            while (read < size) {
                int r = fis.read(bytes, read, size - read);
                if (r < 0) break;
                read += r;
            }
            String json = new String(bytes, 0, read, StandardCharsets.UTF_8);
            return gson.fromJson(json, OfflineBundle.class);
        } catch (IOException | JsonSyntaxException e) {
            Log.w(TAG, "read failed", e);
            return null;
        }
    }

    public boolean exists() {
        return new File(context.getFilesDir(), FILE_NAME).exists();
    }

    public Activity getActivityById(String activityId) {
        if (activityId == null) return null;
        OfflineBundle bundle = read();
        if (bundle == null || bundle.getActivities() == null) return null;
        for (Activity a : bundle.getActivities()) {
            if (activityId.equals(a.getId())) return a;
        }
        return null;
    }
}
