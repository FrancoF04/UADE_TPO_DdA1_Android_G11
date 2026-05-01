package com.example.androidapp.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class TokenManager {
    private static final String PREF_NAME = "secure_user_prefs";

    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ACCESS_EXPIRES_AT = "access_expires_at";
    private static final String KEY_REFRESH_EXPIRES_AT = "refresh_expires_at";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_BIOMETRIC_OPT_IN_DISMISSED = "biometric_opt_in_dismissed";

    private SharedPreferences sharedPreferences;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    // --- Tokens ---

    public void saveSession(String accessToken,
                            String refreshToken,
                            long accessExpiresAtEpochMs,
                            long refreshExpiresAtEpochMs) {
        sharedPreferences.edit()
                .putString(KEY_AUTH_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_ACCESS_EXPIRES_AT, accessExpiresAtEpochMs)
                .putLong(KEY_REFRESH_EXPIRES_AT, refreshExpiresAtEpochMs)
                .apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    public long getAccessExpiresAt() {
        return sharedPreferences.getLong(KEY_ACCESS_EXPIRES_AT, 0L);
    }

    public long getRefreshExpiresAt() {
        return sharedPreferences.getLong(KEY_REFRESH_EXPIRES_AT, 0L);
    }

    public boolean isAccessTokenValid() {
        String token = getToken();
        return token != null && System.currentTimeMillis() < getAccessExpiresAt();
    }

    public boolean isRefreshTokenValid() {
        String token = getRefreshToken();
        return token != null && System.currentTimeMillis() < getRefreshExpiresAt();
    }

    public void clearSession() {
        sharedPreferences.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_EXPIRES_AT)
                .remove(KEY_REFRESH_EXPIRES_AT)
                .apply();
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    public void saveToken(String token) {
        sharedPreferences.edit().putString(KEY_AUTH_TOKEN, token).apply();
    }

    public void clearToken() {
        sharedPreferences.edit().remove(KEY_AUTH_TOKEN).apply();
    }

    // --- Biometric flags ---

    public boolean isBiometricEnabled() {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricOptInDismissed() {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_OPT_IN_DISMISSED, false);
    }

    public void setBiometricOptInDismissed(boolean dismissed) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_OPT_IN_DISMISSED, dismissed).apply();
    }
}
