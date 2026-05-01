package com.example.androidapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class PreferencesStore {

    private static final String PREF_NAME = "user_preferences_cache";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_DESTINATIONS = "destinations";
    private static final String KEY_LAST_SYNCED_AT = "last_synced_at";

    private final SharedPreferences prefs;

    @Inject
    public PreferencesStore(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> getCategories() {
        return new HashSet<>(prefs.getStringSet(KEY_CATEGORIES, new HashSet<>()));
    }

    public Set<String> getDestinations() {
        return new HashSet<>(prefs.getStringSet(KEY_DESTINATIONS, new HashSet<>()));
    }

    public long getLastSyncedAt() {
        return prefs.getLong(KEY_LAST_SYNCED_AT, 0L);
    }

    public boolean hasAnyPreference() {
        return !getCategories().isEmpty() || !getDestinations().isEmpty();
    }

    public void save(List<String> categories, List<String> destinations) {
        Set<String> cats = new HashSet<>(categories != null ? categories : new HashSet<>());
        Set<String> dests = new HashSet<>(destinations != null ? destinations : new HashSet<>());
        prefs.edit()
                .putStringSet(KEY_CATEGORIES, cats)
                .putStringSet(KEY_DESTINATIONS, dests)
                .putLong(KEY_LAST_SYNCED_AT, System.currentTimeMillis())
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
