package com.example.androidapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.androidapp.data.model.User;

public class SessionManager {

    private static final String PREF_NAME = "desapp_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_FULL_NAME = "user_full_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, User user) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_NAME, user.getUsername())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_FULL_NAME, user.getFullName())
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getBearerToken() {
        return "Bearer " + getToken();
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getUserFullName() {
        return prefs.getString(KEY_USER_FULL_NAME, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
