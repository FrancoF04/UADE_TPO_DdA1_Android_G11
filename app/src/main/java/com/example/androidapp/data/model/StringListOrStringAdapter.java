package com.example.androidapp.data.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles API fields that may come as a single string, an array of strings,
 * or an array of objects with a date-like field.
 */
public class StringListOrStringAdapter extends TypeAdapter<List<String>> {

    @Override
    public void write(JsonWriter out, List<String> value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginArray();
        for (String item : value) {
            out.value(item);
        }
        out.endArray();
    }

    @Override
    public List<String> read(JsonReader in) throws IOException {
        JsonToken token = in.peek();

        if (token == JsonToken.NULL) {
            in.nextNull();
            return Collections.emptyList();
        }

        if (token == JsonToken.STRING) {
            return Collections.singletonList(in.nextString());
        }

        JsonElement root = JsonParser.parseReader(in);
        if (!root.isJsonArray()) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray()) {
            if (element == null || element.isJsonNull()) {
                continue;
            }

            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
                continue;
            }

            if (element.isJsonObject()) {
                String objectDate = extractDateFromObject(element.getAsJsonObject());
                if (objectDate != null && !objectDate.isEmpty()) {
                    values.add(objectDate);
                }
            }
        }

        return values;
    }

    private String extractDateFromObject(JsonObject jsonObject) {
        if (jsonObject.has("date") && !jsonObject.get("date").isJsonNull()) {
            return jsonObject.get("date").getAsString();
        }
        if (jsonObject.has("startDate") && !jsonObject.get("startDate").isJsonNull()) {
            return jsonObject.get("startDate").getAsString();
        }
        if (jsonObject.has("value") && !jsonObject.get("value").isJsonNull()) {
            return jsonObject.get("value").getAsString();
        }
        return null;
    }
}
