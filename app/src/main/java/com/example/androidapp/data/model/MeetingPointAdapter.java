package com.example.androidapp.data.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MeetingPointAdapter extends TypeAdapter<MeetingPoint> {

    @Override
    public void write(JsonWriter out, MeetingPoint value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        out.name("latitude").value(value.getLatitude());
        out.name("longitude").value(value.getLongitude());
        out.name("address").value(value.getAddress());
        out.endObject();
    }

    @Override
    public MeetingPoint read(JsonReader in) throws IOException {
        JsonToken token = in.peek();

        if (token == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        if (token == JsonToken.STRING) {
            String address = in.nextString();
            return new MeetingPoint(0, 0, address);
        }

        double latitude = 0;
        double longitude = 0;
        String address = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case "latitude":
                    latitude = in.nextDouble();
                    break;
                case "longitude":
                    longitude = in.nextDouble();
                    break;
                case "address":
                    address = in.nextString();
                    break;
                default:
                    in.skipValue();
                    break;
            }
        }
        in.endObject();

        return new MeetingPoint(latitude, longitude, address);
    }
}
