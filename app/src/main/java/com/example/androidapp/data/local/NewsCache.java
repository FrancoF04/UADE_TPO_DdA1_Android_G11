package com.example.androidapp.data.local;

import android.content.Context;
import android.util.Log;

import com.example.androidapp.data.model.News;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class NewsCache {

    private static final String TAG = "NewsCache";
    private static final String FILE_NAME = "news_cache.json";

    private final Context context;
    private final Gson gson;

    @Inject
    public NewsCache(@ApplicationContext Context context, Gson gson) {
        this.context = context;
        this.gson = gson;
    }

    public void save(List<News> items) {
        String json = gson.toJson(items);
        try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "save failed", e);
        }
    }

    public List<News> read() {
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
            Type t = new TypeToken<List<News>>(){}.getType();
            return gson.fromJson(json, t);
        } catch (IOException | JsonSyntaxException e) {
            Log.w(TAG, "read failed", e);
            return null;
        }
    }

    public boolean exists() {
        return new File(context.getFilesDir(), FILE_NAME).exists();
    }
}
