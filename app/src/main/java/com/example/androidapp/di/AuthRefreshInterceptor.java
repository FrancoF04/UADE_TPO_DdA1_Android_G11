package com.example.androidapp.di;

import com.example.androidapp.BuildConfig;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.util.SessionEventBus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Interceptor;

@Singleton
public class AuthRefreshInterceptor implements Interceptor {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final TokenManager tokenManager;
    private final SessionEventBus sessionEventBus;
    private final Object refreshLock = new Object();

    private final OkHttpClient bareClient;

    @Inject
    public AuthRefreshInterceptor(TokenManager tokenManager, SessionEventBus sessionEventBus) {
        this.tokenManager = tokenManager;
        this.sessionEventBus = sessionEventBus;
        this.bareClient = new OkHttpClient.Builder().build();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String accessToken = tokenManager.getToken();

        Request original = chain.request();
        Request authed = original.newBuilder()
                .header("Authorization", accessToken != null ? "Bearer " + accessToken : "")
                .build();
        Response response = chain.proceed(authed);

        if (response.code() != 401) {
            return response;
        }

        synchronized (refreshLock) {
            String currentAccess = tokenManager.getToken();

            if (currentAccess != null && !currentAccess.equals(accessToken)) {
                response.close();
                Request retry = original.newBuilder()
                        .header("Authorization", "Bearer " + currentAccess)
                        .build();
                return chain.proceed(retry);
            }

            if (!tokenManager.isRefreshTokenValid()) {
                sessionEventBus.notifySessionExpired();
                return response;
            }

            String newAccess = doRefresh(tokenManager.getRefreshToken());
            if (newAccess == null) {
                tokenManager.clearSession();
                sessionEventBus.notifySessionExpired();
                return response;
            }

            response.close();
            Request retry = original.newBuilder()
                    .header("Authorization", "Bearer " + newAccess)
                    .build();
            return chain.proceed(retry);
        }
    }

    private String doRefresh(String refreshToken) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("refreshToken", refreshToken);

            Request request = new Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "auth/refresh")
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response refreshResponse = bareClient.newCall(request).execute()) {
                if (!refreshResponse.isSuccessful()) {
                    return null;
                }
                ResponseBody respBody = refreshResponse.body();
                if (respBody == null) return null;

                JsonObject parsed = new Gson().fromJson(respBody.string(), JsonObject.class);
                if (parsed == null || !parsed.has("data") || parsed.get("data").isJsonNull()) {
                    return null;
                }
                JsonObject data = parsed.getAsJsonObject("data");
                String newAccess = data.has("token") ? data.get("token").getAsString() : null;
                String newRefresh = data.has("refreshToken") ? data.get("refreshToken").getAsString() : null;
                if (newAccess == null || newRefresh == null) return null;

                long now = System.currentTimeMillis();
                long accessExpiresAt = data.has("expiresAt")
                        ? parseIsoToEpoch(data.get("expiresAt").getAsString(), now + 60L * 60L * 1000L)
                        : now + 60L * 60L * 1000L;
                long refreshExpiresAt = data.has("refreshExpiresAt")
                        ? parseIsoToEpoch(data.get("refreshExpiresAt").getAsString(), now + 7L * 24L * 60L * 60L * 1000L)
                        : now + 7L * 24L * 60L * 60L * 1000L;

                tokenManager.saveSession(newAccess, newRefresh, accessExpiresAt, refreshExpiresAt);
                return newAccess;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static long parseIsoToEpoch(String iso, long fallback) {
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception e) {
            return fallback;
        }
    }
}
