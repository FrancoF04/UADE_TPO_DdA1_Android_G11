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
 * Si no parsea, devuelve un fallback generico.
 */
public final class ApiErrorParser {

    private static final String FALLBACK = "Algo fallo, reintenta en unos segundos";

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
