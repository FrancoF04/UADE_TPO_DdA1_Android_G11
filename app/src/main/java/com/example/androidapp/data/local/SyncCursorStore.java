package com.example.androidapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Persiste el cursor {@code since} del long-polling de cambios de reservas y las
 * claves de cambios ya notificados, para no volver a avisar lo mismo tras un
 * reinicio de la app o una reconexión.
 */
@Singleton
public class SyncCursorStore {

    private static final String PREFS = "sync_cursor_store";
    private static final String KEY_SINCE = "since";
    private static final String KEY_PROCESSED = "processed";

    private final SharedPreferences prefs;

    @Inject
    public SyncCursorStore(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getSince() {
        return prefs.getString(KEY_SINCE, null);
    }

    public void setSince(String since) {
        if (since != null) {
            prefs.edit().putString(KEY_SINCE, since).apply();
        }
    }

    public boolean isProcessed(String key) {
        return prefs.getStringSet(KEY_PROCESSED, new HashSet<>()).contains(key);
    }

    public void markProcessed(String key) {
        Set<String> current = new HashSet<>(prefs.getStringSet(KEY_PROCESSED, new HashSet<>()));
        current.add(key);
        prefs.edit().putStringSet(KEY_PROCESSED, current).apply();
    }
}
