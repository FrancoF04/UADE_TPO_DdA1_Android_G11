# Plan A — Mobile Auth (Consigna 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar la consigna 1 del TPO XploreNow en mobile: OTP resend con cooldown, opt-in biométrico post-login, auto-prompt biométrico al abrir la app con refresh transparente, manejo de sesión vencida con user+pass forzado.

**Architecture:** Java + Hilt + Retrofit + EncryptedSharedPreferences. Se extiende `TokenManager` con flags biométricos y TTLs, se agrega `BiometricHelper` (wrapper de `BiometricManager`/`BiometricPrompt`), `SessionEventBus` Java puro (singleton Hilt) que reemplaza `BroadcastReceiver` para notificar SESSION_EXPIRED, y `AuthRefreshInterceptor` que hace refresh transparente bloqueante al recibir 401. `MainActivity` decide el destino inicial según el estado de tokens.

**Tech Stack:** Java 11, Hilt 2.59.2, Retrofit 2.11.0, OkHttp 4.12.0, AndroidX Security-Crypto 1.1.0-alpha06, AndroidX Biometric 1.2.0-alpha05 (nueva), Navigation 2.8.9, AndroidX AppCompat. Min SDK 30, Target SDK 36.

**Spec base:** `docs/superpowers/specs/2026-04-30-mobile-auth-catalog-design.md`

**Working directory:** `C:/Users/a950839/OneDrive - ATOS/Dev/desapp/mobile-app-android`

**Branch base:** `main`. Trabajar en branch `feature/auth-consigna-1` (crear al inicio).

---

## File structure

**Nuevos** (`app/src/main/java/com/example/androidapp/`):

| Archivo | Responsabilidad |
|---|---|
| `util/SessionEventBus.java` | Singleton Hilt; bus de eventos `SESSION_EXPIRED` con listeners registrables |
| `util/SessionExpiredListener.java` | Interface con `onSessionExpired()` |
| `util/BiometricStatus.java` | Enum `{ AVAILABLE, NOT_ENROLLED, NO_HARDWARE, UNAVAILABLE }` |
| `util/BiometricCanAuthMapper.java` | Helper puro: `int code → BiometricStatus` |
| `util/BiometricHelper.java` | Wrapper de `BiometricManager` y `BiometricPrompt` |
| `util/OtpResendCooldown.java` | Helper puro de countdown 30s |
| `util/ApiErrorParser.java` | Helper: extrae `error` string del response body |
| `di/AuthRefreshInterceptor.java` | Interceptor OkHttp con refresh transparente bloqueante |
| `ui/auth/BiometricOptInDialog.java` | `AlertDialog` post-login para activar bio |

**Modificados:**

| Archivo | Cambio |
|---|---|
| `gradle/libs.versions.toml` | Agregar `androidx.biometric:biometric` |
| `app/build.gradle.kts` | Agregar `implementation(libs.biometric)` |
| `data/local/TokenManager.java` | Refresh token, TTLs, biometric flags, helpers de validez |
| `di/NetworkModule.java` | Wire `AuthRefreshInterceptor` |
| `MainActivity.java` | Boot decision + register `SessionExpiredListener` |
| `ui/auth/LoginFragment.java` | Args `autoPromptBiometric`, `forceUserPass`; opt-in dialog post-login |
| `ui/auth/OtpFragment.java` | Botón "Reenviar código" con cooldown |
| `ui/profile/ProfileFragment.java` | Toggle biométrico |

**Notas:**
- TDD no aplica: PROJECT_CONTEXT no incluye testing automatizado. Cada task termina con build verification (`./gradlew assembleDebug`) y, donde aplique, smoke test manual en device.
- Commits en inglés, sin co-author. Author email `otros@mauricioantolin.com` (pasar `-c user.email=...` en cada commit).
- Cada task se commitea por separado para mantener historia revisable.

---

## Task 1: Setup branch + add Biometric dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Crear branch desde main**

```bash
git checkout main
git pull --ff-only origin main
git checkout -b feature/auth-consigna-1
```

- [ ] **Step 2: Agregar versión y dependencia en `gradle/libs.versions.toml`**

En la sección `[versions]`, después de `okhttp = "4.12.0"`, agregar:

```toml
biometric = "1.2.0-alpha05"
```

En la sección `[libraries]`, después de la línea de `okhttp = ...`, agregar:

```toml
biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
```

- [ ] **Step 3: Agregar dependencia en `app/build.gradle.kts`**

Dentro del bloque `dependencies { ... }`, después de `implementation(libs.okhttp)`, agregar:

```kotlin
implementation(libs.biometric)
```

- [ ] **Step 4: Sync + build**

```bash
./gradlew assembleDebug --warning-mode all
```

Expected: BUILD SUCCESSFUL. Si falla con "library not found", correr `./gradlew --refresh-dependencies` y reintentar.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "build: add androidx.biometric dependency"
```

---

## Task 2: Add SessionExpiredListener interface

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/SessionExpiredListener.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.util;

/**
 * Implementado por componentes UI que necesitan reaccionar a una sesión vencida
 * (refresh token inválido o ausente). El AuthRefreshInterceptor dispara la notificación
 * a través de SessionEventBus cuando detecta que ya no se puede recuperar la sesión.
 */
public interface SessionExpiredListener {
    void onSessionExpired();
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/SessionExpiredListener.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat: add SessionExpiredListener interface"
```

---

## Task 3: Add SessionEventBus singleton

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/SessionEventBus.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Bus interno para SESSION_EXPIRED. El AuthRefreshInterceptor llama
 * notifySessionExpired() cuando el refresh token está inválido. Los
 * listeners registrados (típicamente MainActivity en onResume) reciben
 * el callback en el thread donde se disparó (los receptores deben usar
 * runOnUiThread si tocan UI).
 */
@Singleton
public class SessionEventBus {

    private final List<SessionExpiredListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    public SessionEventBus() {
        // Hilt-managed
    }

    public void register(SessionExpiredListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(SessionExpiredListener listener) {
        listeners.remove(listener);
    }

    public void notifySessionExpired() {
        for (SessionExpiredListener listener : listeners) {
            try {
                listener.onSessionExpired();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/SessionEventBus.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat: add SessionEventBus singleton for session-expired notifications"
```

---

## Task 4: Extend TokenManager with refresh token, TTLs and biometric flags

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/data/local/TokenManager.java`

- [ ] **Step 1: Reemplazar el archivo completo**

```java
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

    /** Borra access + refresh tokens y sus TTLs. Mantiene flags biométricos. */
    public void clearSession() {
        sharedPreferences.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_EXPIRES_AT)
                .remove(KEY_REFRESH_EXPIRES_AT)
                .apply();
    }

    /** Borra todo. Solo usar en logout completo o wipe. */
    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    // Compat: mantenemos saveToken/clearToken para no romper callers existentes.
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
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. Si hay errores en otros archivos por el singleton estático eliminado (`TokenManager.getInstance(...)` removido), todos los callsites deben usar `@Inject TokenManager`. Buscar con:

```bash
grep -rn "TokenManager.getInstance" app/src/main/java
```

Si no aparece ningún match, está OK.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/data/local/TokenManager.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): extend TokenManager with refresh token, TTLs and biometric flags"
```

---

## Task 5: Add BiometricStatus enum

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/BiometricStatus.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.util;

/**
 * Resultado consolidado de BiometricManager.canAuthenticate().
 * - AVAILABLE: hardware presente y al menos una credencial enrolada — biometric flow OK.
 * - NOT_ENROLLED: hardware presente pero el usuario no enroló nada todavía — ofrecer Settings.ACTION_BIOMETRIC_ENROLL.
 * - NO_HARDWARE: dispositivo sin hardware biométrico — feature no disponible.
 * - UNAVAILABLE: hardware temporalmente inaccesible o requiere update de seguridad — feature no disponible.
 */
public enum BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/BiometricStatus.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): add BiometricStatus enum"
```

---

## Task 6: Add BiometricCanAuthMapper

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/BiometricCanAuthMapper.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.util;

import androidx.biometric.BiometricManager;

/**
 * Mapea el código retornado por BiometricManager.canAuthenticate(int authenticators)
 * a un BiometricStatus consumible por la UI.
 */
public final class BiometricCanAuthMapper {

    private BiometricCanAuthMapper() {}

    public static BiometricStatus map(int code) {
        switch (code) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return BiometricStatus.AVAILABLE;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return BiometricStatus.NOT_ENROLLED;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return BiometricStatus.NO_HARDWARE;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
            default:
                return BiometricStatus.UNAVAILABLE;
        }
    }
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/BiometricCanAuthMapper.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): add BiometricCanAuthMapper helper"
```

---

## Task 7: Add BiometricHelper

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/BiometricHelper.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.util;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class BiometricHelper {

    public interface OnSuccess { void onSuccess(); }
    public interface OnError { void onError(int errorCode, CharSequence errorMessage); }

    private final Context appContext;

    @Inject
    public BiometricHelper(@ApplicationContext Context appContext) {
        this.appContext = appContext;
    }

    public BiometricStatus checkAvailability() {
        BiometricManager manager = BiometricManager.from(appContext);
        int code = manager.canAuthenticate(allowedAuthenticators());
        return BiometricCanAuthMapper.map(code);
    }

    public void promptForAuth(FragmentActivity activity,
                              String title,
                              String subtitle,
                              OnSuccess onSuccess,
                              OnError onError) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(
                activity,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        onSuccess.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errorMessage) {
                        onError.onError(errorCode, errorMessage);
                    }
                }
        );

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(allowedAuthenticators())
                .build();

        prompt.authenticate(info);
    }

    public Intent enrollIntent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
            intent.putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    allowedAuthenticators()
            );
            return intent;
        }
        return new Intent(Settings.ACTION_SECURITY_SETTINGS);
    }

    private static int allowedAuthenticators() {
        return Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL;
    }
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/BiometricHelper.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): add BiometricHelper wrapping BiometricManager and BiometricPrompt"
```

---

## Task 8: Add OtpResendCooldown helper

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/OtpResendCooldown.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.util;

/**
 * Helper puro de countdown del botón "Reenviar código".
 * Cooldown duration: 30s.
 */
public final class OtpResendCooldown {

    public static final long COOLDOWN_MS = 30_000L;

    private long lastResendAtEpochMs = 0L;

    public void markResendNow(long nowEpochMs) {
        this.lastResendAtEpochMs = nowEpochMs;
    }

    public long secondsRemaining(long nowEpochMs) {
        long elapsed = nowEpochMs - lastResendAtEpochMs;
        long remaining = COOLDOWN_MS - elapsed;
        if (remaining <= 0) return 0L;
        return (remaining + 999L) / 1000L;
    }

    public boolean canResend(long nowEpochMs) {
        return secondsRemaining(nowEpochMs) == 0;
    }

    public long getLastResendAtEpochMs() {
        return lastResendAtEpochMs;
    }

    public void restoreFrom(long lastResendAtEpochMs) {
        this.lastResendAtEpochMs = lastResendAtEpochMs;
    }
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/OtpResendCooldown.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): add OtpResendCooldown helper"
```

---

## Task 9: Add ApiErrorParser helper

**Files:**
- Create: `app/src/main/java/com/example/androidapp/util/ApiErrorParser.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Extrae el mensaje de error de un Response<?> de Retrofit.
 * Asume formato del backend: { "success": false, "error": "Mensaje" }.
 * Si no parsea, devuelve un fallback genérico.
 */
public final class ApiErrorParser {

    private static final String FALLBACK = "Algo falló, reintentá en unos segundos";

    private ApiErrorParser() {}

    public static String extractMessage(Response<?> response) {
        if (response == null) return FALLBACK;

        ResponseBody body = response.errorBody();
        if (body == null) return FALLBACK;

        String raw;
        try {
            raw = body.string();
        } catch (IOException e) {
            return FALLBACK;
        }
        if (raw == null || raw.trim().isEmpty()) return FALLBACK;

        try {
            JsonObject obj = new Gson().fromJson(raw, JsonObject.class);
            if (obj != null && obj.has("error") && !obj.get("error").isJsonNull()) {
                String msg = obj.get("error").getAsString();
                if (msg != null && !msg.trim().isEmpty()) {
                    return msg;
                }
            }
        } catch (JsonSyntaxException ignored) {
            // body no era JSON; cae al fallback
        }

        return FALLBACK;
    }
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/util/ApiErrorParser.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat: add ApiErrorParser helper"
```

---

## Task 10: Add AuthRefreshInterceptor

**Files:**
- Create: `app/src/main/java/com/example/androidapp/di/AuthRefreshInterceptor.java`

**Pre-requisito:** este interceptor llama al endpoint `/auth/refresh` directamente con OkHttp + Gson (sin Retrofit) para evitar dependencia circular con el cliente HTTP que él mismo intercepta.

- [ ] **Step 1: Crear el archivo**

```java
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

/**
 * Interceptor de OkHttp que:
 *  1. Adjunta Authorization: Bearer <accessToken> al request.
 *  2. Si la response es 401, intenta refrescar el access token con el refreshToken.
 *     - Si refresh OK → reintenta el request original con el nuevo token (una sola vez).
 *     - Si refresh falla → notifica SESSION_EXPIRED y devuelve la response 401 original.
 *  3. Usa un lock para que N requests concurrentes con 401 disparen UN solo refresh.
 */
@Singleton
public class AuthRefreshInterceptor implements Interceptor {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final TokenManager tokenManager;
    private final SessionEventBus sessionEventBus;
    private final Object refreshLock = new Object();

    // Cliente HTTP minimalista solo para llamar al refresh endpoint, sin interceptors,
    // para evitar recursión infinita.
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

            // Si otro thread ya refrescó mientras esperaba el lock, reintentar con el nuevo token.
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
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/di/AuthRefreshInterceptor.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): add AuthRefreshInterceptor with transparent refresh and lock"
```

---

## Task 11: Wire AuthRefreshInterceptor in NetworkModule

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/di/NetworkModule.java`

**Pre-requisito:** leer el archivo actual antes de modificar.

- [ ] **Step 1: Leer el archivo actual**

```bash
cat app/src/main/java/com/example/androidapp/di/NetworkModule.java
```

Anotar la estructura: hoy debería tener un `@Provides` para `OkHttpClient` con un interceptor que agrega el header `Authorization: Bearer ...`. Hay que **reemplazar** ese interceptor manual por el `AuthRefreshInterceptor` inyectado.

- [ ] **Step 2: Modificar el `@Provides` de `OkHttpClient`**

Localizar el método que provee `OkHttpClient` (típicamente `provideOkHttpClient(...)`). Reemplazar su firma y body:

```java
@Provides
@Singleton
public OkHttpClient provideOkHttpClient(AuthRefreshInterceptor authInterceptor) {
    return new OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build();
}
```

Importante:
- **Eliminar** cualquier interceptor manual previo que solo agregaba el header `Authorization` — `AuthRefreshInterceptor` ya hace eso.
- **Eliminar** el parámetro `TokenManager tokenManager` del método si solo se usaba para construir el viejo interceptor.
- **Mantener** cualquier `HttpLoggingInterceptor` existente si lo había.

- [ ] **Step 3: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. Si hay error de Hilt sobre dependencias no encontradas, asegurar que `AuthRefreshInterceptor` tiene `@Singleton` + `@Inject` en su constructor (Task 10 lo cubre).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/androidapp/di/NetworkModule.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "refactor(auth): wire AuthRefreshInterceptor in NetworkModule"
```

---

## Task 12: Add BiometricOptInDialog

**Files:**
- Create: `app/src/main/java/com/example/androidapp/ui/auth/BiometricOptInDialog.java`

- [ ] **Step 1: Crear el archivo**

```java
package com.example.androidapp.ui.auth;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.example.androidapp.data.local.TokenManager;

/**
 * Dialog post-login para invitar al usuario a activar biometría.
 * Solo se muestra si BiometricHelper.checkAvailability() == AVAILABLE
 * y biometric_opt_in_dismissed == false y biometric_enabled == false.
 */
public final class BiometricOptInDialog {

    public interface OnActivate { void onActivate(); }

    private BiometricOptInDialog() {}

    public static void show(Context context, TokenManager tokenManager, OnActivate onActivate) {
        new AlertDialog.Builder(context)
                .setTitle("¿Activar ingreso con huella?")
                .setMessage("La próxima vez que abras la app podés ingresar con tu huella en lugar de escribir usuario y contraseña.")
                .setPositiveButton("Activar", (dialog, which) -> {
                    tokenManager.setBiometricEnabled(true);
                    if (onActivate != null) onActivate.onActivate();
                })
                .setNegativeButton("Ahora no", (dialog, which) -> {
                    tokenManager.setBiometricOptInDismissed(true);
                })
                .setCancelable(false)
                .show();
    }
}
```

- [ ] **Step 2: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/auth/BiometricOptInDialog.java
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): add BiometricOptInDialog post-login"
```

---

## Task 13: MainActivity boot decision + SessionEventBus listener

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/MainActivity.java`
- Modify: `app/src/main/res/navigation/nav_graph.xml` (si tiene `app:startDestination` hardcoded)

**Pre-requisito:** leer `MainActivity.java` y `nav_graph.xml` actuales.

- [ ] **Step 1: Leer estado actual**

```bash
cat app/src/main/java/com/example/androidapp/MainActivity.java
cat app/src/main/res/navigation/nav_graph.xml | head -30
```

- [ ] **Step 2: Modificar `MainActivity.java`**

Convertir la firma de la clase a `implements SessionExpiredListener`:

```java
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements SessionExpiredListener {

    @Inject TokenManager tokenManager;
    @Inject SessionEventBus sessionEventBus;

    private NavController navController;
```

En `onCreate`, **después** de obtener el `NavController` y **antes** de cualquier `setupWith*`, agregar:

```java
NavInflater inflater = navController.getNavInflater();
NavGraph graph = inflater.inflate(R.navigation.nav_graph);

if (tokenManager.getToken() == null) {
    graph.setStartDestination(R.id.loginFragment);
    navController.setGraph(graph);
} else if (tokenManager.isAccessTokenValid()) {
    graph.setStartDestination(R.id.homeFragment);
    navController.setGraph(graph);
} else if (tokenManager.isRefreshTokenValid() && tokenManager.isBiometricEnabled()) {
    graph.setStartDestination(R.id.loginFragment);
    Bundle args = new Bundle();
    args.putBoolean("autoPromptBiometric", true);
    navController.setGraph(graph, args);
} else {
    graph.setStartDestination(R.id.loginFragment);
    Bundle args = new Bundle();
    args.putBoolean("forceUserPass", true);
    navController.setGraph(graph, args);
}
```

Importante: si `nav_graph.xml` ya tiene `app:startDestination`, removerlo (o el código de arriba lo va a sobrescribir).

Agregar `onResume`, `onPause` y `onSessionExpired`:

```java
@Override
protected void onResume() {
    super.onResume();
    sessionEventBus.register(this);
}

@Override
protected void onPause() {
    super.onPause();
    sessionEventBus.unregister(this);
}

@Override
public void onSessionExpired() {
    runOnUiThread(() -> {
        Bundle args = new Bundle();
        args.putBoolean("forceUserPass", true);
        navController.popBackStack(R.id.loginFragment, false);
        navController.navigate(R.id.loginFragment, args);
    });
}
```

Imports a agregar (si no estaban):

```java
import android.os.Bundle;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import com.example.androidapp.R;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.util.SessionEventBus;
import com.example.androidapp.util.SessionExpiredListener;
import javax.inject.Inject;
```

- [ ] **Step 3: Build verification**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL. Si hay error de "loginFragment id not found", verificar que el ID exista en `nav_graph.xml`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/androidapp/MainActivity.java app/src/main/res/navigation/nav_graph.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): MainActivity decides start destination by token state and listens for session-expired"
```

---

## Task 14: LoginFragment auto-prompt biometric + opt-in dialog post-login + forceUserPass

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/auth/LoginFragment.java`
- Modify: `app/src/main/res/layout/fragment_login.xml`

**Pre-requisito:** leer `LoginFragment.java` actual.

- [ ] **Step 1: Leer estado actual**

```bash
cat app/src/main/java/com/example/androidapp/ui/auth/LoginFragment.java
cat app/src/main/res/layout/fragment_login.xml | head -40
```

Anotar dónde están los inputs de user/pass, el botón "Ingresar con OTP", y la lambda de `onResponse` exitoso del login.

- [ ] **Step 2: Agregar TextView de mensaje en `fragment_login.xml`**

En el layout, arriba de los inputs de usuario, agregar:

```xml
<TextView
    android:id="@+id/tvSessionExpired"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:visibility="gone"
    android:text="Tu sesión expiró, ingresá con tu usuario y contraseña"
    android:textColor="#B91C1C"
    android:padding="12dp"
    android:gravity="center"/>
```

- [ ] **Step 3: Modificar `LoginFragment.java`**

Agregar imports:

```java
import com.example.androidapp.util.BiometricHelper;
import com.example.androidapp.util.BiometricStatus;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.data.model.AuthResponse;
import com.example.androidapp.data.model.ApiResponse;
import javax.inject.Inject;
```

Inyectar dependencias:

```java
@Inject TokenManager tokenManager;
@Inject BiometricHelper biometricHelper;
@Inject AuthApi authApi;
```

En `onViewCreated`, después de bindear el layout:

```java
boolean autoPromptBiometric = getArguments() != null && getArguments().getBoolean("autoPromptBiometric", false);
boolean forceUserPass = getArguments() != null && getArguments().getBoolean("forceUserPass", false);

if (forceUserPass) {
    binding.tvSessionExpired.setVisibility(View.VISIBLE);
    if (binding.btnOtp != null) binding.btnOtp.setVisibility(View.GONE);
}

if (autoPromptBiometric && biometricHelper.checkAvailability() == BiometricStatus.AVAILABLE) {
    biometricHelper.promptForAuth(
            requireActivity(),
            "Ingresar a XploreNow",
            "Autenticate con tu huella",
            this::onBiometricSuccess,
            (code, msg) -> { /* dejar la pantalla normal visible */ }
    );
}
```

Agregar `onBiometricSuccess()`:

```java
private void onBiometricSuccess() {
    String refresh = tokenManager.getRefreshToken();
    if (refresh == null) return;

    java.util.Map<String, String> body = new java.util.HashMap<>();
    body.put("refreshToken", refresh);

    authApi.refresh(body).enqueue(new retrofit2.Callback<ApiResponse<AuthResponse>>() {
        @Override
        public void onResponse(retrofit2.Call<ApiResponse<AuthResponse>> call,
                               retrofit2.Response<ApiResponse<AuthResponse>> response) {
            if (!isAdded()) return;
            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                AuthResponse data = response.body().getData();
                long now = System.currentTimeMillis();
                tokenManager.saveSession(
                        data.getToken(),
                        data.getRefreshToken(),
                        now + 60L * 60L * 1000L,
                        now + 7L * 24L * 60L * 60L * 1000L
                );
                androidx.navigation.fragment.NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_login_to_home);
            }
        }

        @Override
        public void onFailure(retrofit2.Call<ApiResponse<AuthResponse>> call, Throwable t) {
            // dejar la pantalla normal visible
        }
    });
}
```

Modificar el callback de **login user+pass exitoso** (buscar el callsite donde se hace `navController.navigate(... home ...)` después de un login OK):

1. Reemplazar `tokenManager.saveToken(...)` por `tokenManager.saveSession(token, refreshToken, accessExpiresAt, refreshExpiresAt)`. Si `AuthResponse` no tiene `accessExpiresAt`/`refreshExpiresAt`, calcular con defaults: `now + 60min` / `now + 7d`.
2. **Antes** del `navigate`, mostrar el opt-in dialog si corresponde:

```java
if (biometricHelper.checkAvailability() == BiometricStatus.AVAILABLE
        && !tokenManager.isBiometricEnabled()
        && !tokenManager.isBiometricOptInDismissed()) {
    BiometricOptInDialog.show(requireContext(), tokenManager, () -> {});
}
NavHostFragment.findNavController(this).navigate(R.id.action_login_to_home);
```

- [ ] **Step 4: Build + smoke manual en device**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual:
1. Wipe data del app (`adb shell pm clear com.example.androidapp`)
2. Login con `juanperez` / `password123`
3. Verificar dialog "¿Activar ingreso con huella?" si el device tiene biometría enrolada
4. Tap "Activar" → toast + navega a Home
5. Cerrar app → reabrir → debería ver auto-prompt biométrico
6. Pasar la huella → entrar a Home (refresh ocurrió en background)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/auth/LoginFragment.java app/src/main/res/layout/fragment_login.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): LoginFragment auto-prompt biometric and opt-in dialog post-login"
```

---

## Task 15: OtpFragment add resend button with cooldown

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/auth/OtpFragment.java`
- Modify: `app/src/main/res/layout/fragment_otp.xml`

- [ ] **Step 1: Leer estado actual**

```bash
cat app/src/main/java/com/example/androidapp/ui/auth/OtpFragment.java
cat app/src/main/res/layout/fragment_otp.xml | head -30
```

Identificar cómo el fragment recibe el `email` (probablemente vía `getArguments().getString("email")` o un campo guardado).

- [ ] **Step 2: Agregar el botón al layout**

En `fragment_otp.xml`, debajo del input del código y del botón "Verificar", agregar:

```xml
<Button
    android:id="@+id/btnResendOtp"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="12dp"
    android:layout_gravity="center"
    android:text="Reenviar código"
    android:background="?attr/selectableItemBackground"
    android:textColor="#2563EB"/>
```

- [ ] **Step 3: Modificar `OtpFragment.java`**

Agregar imports:

```java
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.example.androidapp.data.model.OtpRequest;
import com.example.androidapp.data.remote.AuthApi;
import com.example.androidapp.util.OtpResendCooldown;
import com.example.androidapp.util.ApiErrorParser;
import javax.inject.Inject;
```

Inyectar `AuthApi`:

```java
@Inject AuthApi authApi;
```

Agregar campos:

```java
private final OtpResendCooldown cooldown = new OtpResendCooldown();
private final Handler cooldownHandler = new Handler(Looper.getMainLooper());
private Runnable cooldownTick;
private static final String STATE_KEY_LAST_RESEND = "last_resend_at";
```

En `onViewCreated`:

```java
if (savedInstanceState != null) {
    cooldown.restoreFrom(savedInstanceState.getLong(STATE_KEY_LAST_RESEND, 0L));
}

binding.btnResendOtp.setOnClickListener(v -> resendOtp());

if (cooldown.secondsRemaining(System.currentTimeMillis()) > 0) {
    startCooldownTick();
}
```

Sobrescribir `onSaveInstanceState`:

```java
@Override
public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putLong(STATE_KEY_LAST_RESEND, cooldown.getLastResendAtEpochMs());
}
```

Sobrescribir `onDestroyView`:

```java
@Override
public void onDestroyView() {
    super.onDestroyView();
    if (cooldownTick != null) cooldownHandler.removeCallbacks(cooldownTick);
}
```

Métodos privados:

```java
private void resendOtp() {
    if (!cooldown.canResend(System.currentTimeMillis())) {
        Toast.makeText(getContext(), "Esperá unos segundos para reenviar", Toast.LENGTH_SHORT).show();
        return;
    }

    // Adaptar este getter al callsite real del fragment — el fragment ya recibe el email
    // de alguna manera (args bundle o campo). Reusar la misma fuente:
    String email = getArguments() != null ? getArguments().getString("email") : null;
    if (email == null || email.trim().isEmpty()) {
        Toast.makeText(getContext(), "No se puede reenviar sin un email válido", Toast.LENGTH_SHORT).show();
        return;
    }

    OtpRequest body = new OtpRequest(email);
    authApi.resendOtp(body).enqueue(new retrofit2.Callback<>() {
        @Override
        public void onResponse(retrofit2.Call call, retrofit2.Response response) {
            if (!isAdded()) return;
            if (response.isSuccessful()) {
                cooldown.markResendNow(System.currentTimeMillis());
                Toast.makeText(getContext(), "Código reenviado", Toast.LENGTH_SHORT).show();
                startCooldownTick();
            } else {
                Toast.makeText(getContext(), ApiErrorParser.extractMessage(response), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(retrofit2.Call call, Throwable t) {
            if (!isAdded()) return;
            Toast.makeText(getContext(), "Sin conexión, reintentá", Toast.LENGTH_SHORT).show();
        }
    });
}

private void startCooldownTick() {
    binding.btnResendOtp.setEnabled(false);
    cooldownTick = new Runnable() {
        @Override
        public void run() {
            if (!isAdded()) return;
            long secs = cooldown.secondsRemaining(System.currentTimeMillis());
            if (secs <= 0) {
                binding.btnResendOtp.setText("Reenviar código");
                binding.btnResendOtp.setEnabled(true);
            } else {
                binding.btnResendOtp.setText("Reenviar (" + secs + "s)");
                cooldownHandler.postDelayed(this, 1000L);
            }
        }
    };
    cooldownHandler.post(cooldownTick);
}
```

**Nota**: si el fragment recibe el email por un mecanismo distinto (ej. campo de instancia poblado en `newInstance()`), reemplazar la línea `String email = getArguments()...` por el getter correcto.

- [ ] **Step 4: Build + smoke manual**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual:
1. En LoginFragment, elegir flow OTP. Pedir código.
2. En OtpFragment, tap "Reenviar código" → toast "Código reenviado" + botón disabled con countdown.
3. Verificar en logs Railway que `console.log("[OTP] Codigo reenviado para...")` aparece.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/auth/OtpFragment.java app/src/main/res/layout/fragment_otp.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): OtpFragment add resend button with 30s cooldown"
```

---

## Task 16: ProfileFragment biometric toggle

**Files:**
- Modify: `app/src/main/java/com/example/androidapp/ui/profile/ProfileFragment.java`
- Modify: `app/src/main/res/layout/fragment_profile.xml`

- [ ] **Step 1: Leer estado actual**

```bash
cat app/src/main/java/com/example/androidapp/ui/profile/ProfileFragment.java
cat app/src/main/res/layout/fragment_profile.xml | head -50
```

- [ ] **Step 2: Agregar fila al layout**

En `fragment_profile.xml`, dentro del LinearLayout vertical de items, agregar:

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="12dp"
    android:gravity="center_vertical">

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Ingresar con huella"
            android:textSize="16sp"/>

        <TextView
            android:id="@+id/tvBiometricSubtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Activá la biometría para tu próximo ingreso"
            android:textSize="12sp"
            android:textColor="#666666"/>
    </LinearLayout>

    <Switch
        android:id="@+id/switchBiometric"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/>
</LinearLayout>
```

- [ ] **Step 3: Modificar `ProfileFragment.java`**

Agregar imports:

```java
import androidx.appcompat.app.AlertDialog;
import com.example.androidapp.data.local.TokenManager;
import com.example.androidapp.util.BiometricHelper;
import com.example.androidapp.util.BiometricStatus;
import javax.inject.Inject;
```

Inyectar:

```java
@Inject TokenManager tokenManager;
@Inject BiometricHelper biometricHelper;
```

Agregar método helper:

```java
private void wireBiometricToggle() {
    BiometricStatus status = biometricHelper.checkAvailability();

    switch (status) {
        case AVAILABLE: {
            binding.switchBiometric.setEnabled(true);
            binding.switchBiometric.setOnCheckedChangeListener(null);
            binding.switchBiometric.setChecked(tokenManager.isBiometricEnabled());
            binding.tvBiometricSubtitle.setText("Activá la biometría para tu próximo ingreso");
            binding.switchBiometric.setOnCheckedChangeListener((view, isChecked) -> {
                if (isChecked) {
                    biometricHelper.promptForAuth(
                            requireActivity(),
                            "Activar huella",
                            "Confirmá tu huella para activar el ingreso biométrico",
                            () -> tokenManager.setBiometricEnabled(true),
                            (code, msg) -> wireBiometricToggle() /* re-sync UI */
                    );
                } else {
                    tokenManager.setBiometricEnabled(false);
                }
            });
            break;
        }
        case NOT_ENROLLED: {
            binding.switchBiometric.setEnabled(true);
            binding.switchBiometric.setOnCheckedChangeListener(null);
            binding.switchBiometric.setChecked(false);
            binding.tvBiometricSubtitle.setText("Necesitás enrolar tu huella en el sistema");
            binding.switchBiometric.setOnCheckedChangeListener((view, isChecked) -> {
                if (isChecked) {
                    binding.switchBiometric.setOnCheckedChangeListener(null);
                    binding.switchBiometric.setChecked(false);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Enrolá tu huella")
                            .setMessage("Para usar biometría tenés que enrolar tu huella en Configuración. ¿Vamos ahora?")
                            .setPositiveButton("Ir", (d, w) -> startActivity(biometricHelper.enrollIntent()))
                            .setNegativeButton("Cancelar", null)
                            .show();
                    wireBiometricToggle();
                }
            });
            break;
        }
        case NO_HARDWARE:
        case UNAVAILABLE:
        default: {
            binding.switchBiometric.setEnabled(false);
            binding.switchBiometric.setOnCheckedChangeListener(null);
            binding.switchBiometric.setChecked(false);
            binding.tvBiometricSubtitle.setText("Tu dispositivo no soporta biometría");
            break;
        }
    }
}
```

En `onViewCreated`, llamar `wireBiometricToggle();`. Y en `onResume` (re-evaluar por si el usuario fue a Settings y enroló):

```java
@Override
public void onResume() {
    super.onResume();
    wireBiometricToggle();
}
```

- [ ] **Step 4: Build + smoke manual**

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Manual:
1. Login → Perfil → ver toggle "Ingresar con huella"
2. Toggle ON → BiometricPrompt → confirmar huella → estado guardado
3. Toggle OFF → estado se desactiva sin prompt
4. En device sin biometría → toggle disabled con texto "no soporta biometría"

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/androidapp/ui/profile/ProfileFragment.java app/src/main/res/layout/fragment_profile.xml
git -c user.email="otros@mauricioantolin.com" -c user.name="Mauricio Antolin" \
    commit -m "feat(auth): ProfileFragment add biometric toggle"
```

---

## Task 17: End-to-end QA + push branch

**Files:** ninguno (validación)

- [ ] **Step 1: Ejecutar checklist QA del spec sección 6 (Auth)**

Correr en device físico, paso a paso:

- [ ] Cold start con tokens limpios → ver Login (sin auto-prompt)
- [ ] Login user+pass válido → ver dialog opt-in biometría (si bio disponible)
- [ ] "Activar" desde dialog → toast + Home
- [ ] Cerrar app → reabrir → auto-prompt biométrico → entrar a Home (verificar logcat: hubo refresh exitoso)
- [ ] Cerrar app con sesión warm (<1h) → reabrir → directo a Home (sin prompt)
- [ ] Esperar >1h y reabrir con bio activada → auto-prompt → refresh → Home
- [ ] Wipe data del app → cold start → Login
- [ ] OTP request → ver código en logs Railway → verify → entrar a Home
- [ ] OTP resend → cooldown 30s en botón
- [ ] Toggle bio en Perfil OFF → cerrar → reabrir → ver Login user+pass (sin auto-prompt)
- [ ] Login en device sin biometría → toggle Perfil disabled con mensaje
- [ ] Login en device con biometría no enrolada → toggle abre AlertDialog → "Ir" abre Settings.ACTION_BIOMETRIC_ENROLL

- [ ] **Step 2: Push branch para PR**

```bash
git push -u origin feature/auth-consigna-1
```

- [ ] **Step 3: Crear PR (manual)**

Abrir GitHub web (o usar `gh pr create`) y crear PR de `feature/auth-consigna-1` → `main` con:

- Título: `feat: cierre consigna 1 (Auth completa con biometría, OTP resend, refresh transparente)`
- Body: copiar el resumen del spec (sección "Decisiones tomadas") y la lista de tasks completadas.

---

## Self-review

| Check | Resultado |
|---|---|
| Cobertura de spec | Cada sub-consigna del spec sección 1 (Auth) tiene una task que la implementa: OTP resend (T15), opt-in (T12+T14), auto-prompt (T7+T13+T14), refresh transparente (T10+T11), sesión vencida → user+pass (T13). |
| Placeholders | El único explícito intencional está en T15: `getArguments()...getString("email")` requiere validar contra el callsite real del fragment. Está marcado como "Adaptar". |
| Type consistency | `BiometricStatus`, `BiometricHelper`, `TokenManager` métodos usados en T13/T14/T16 coinciden con sus declaraciones en T4-T7. `SessionEventBus.notifySessionExpired()` y `register/unregister` coinciden entre T2/T3/T10/T13. |
| TDD | No aplicado por restricción del spec (testing fuera de PROJECT_CONTEXT). Cada task termina con build verification + smoke manual donde aplica. |

---

## Próximo paso

Una vez mergeado este plan, abrir Plan B (`docs/superpowers/plans/2026-04-30-mobile-catalog-consigna-3.md` — pendiente de escribir) que cubre la consigna 3 (Catálogo): cambios al backend (URLs Unsplash), Glide, galería vertical, FiltersFragment, chips/paginación en Home, PreferencesFragment, MapIntentLauncher.
