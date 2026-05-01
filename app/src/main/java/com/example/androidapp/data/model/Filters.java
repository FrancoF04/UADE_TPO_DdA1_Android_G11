package com.example.androidapp.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Filters implements Parcelable {

    public String destination;
    public String category;
    public String date;
    public Integer priceMin;
    public Integer priceMax;

    public Filters() {}

    public boolean isEmpty() {
        return destination == null && category == null && date == null
                && priceMin == null && priceMax == null;
    }

    public void reset() {
        destination = null;
        category = null;
        date = null;
        priceMin = null;
        priceMax = null;
    }

    protected Filters(Parcel in) {
        destination = in.readString();
        category = in.readString();
        date = in.readString();
        priceMin = (Integer) in.readValue(Integer.class.getClassLoader());
        priceMax = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(destination);
        dest.writeString(category);
        dest.writeString(date);
        dest.writeValue(priceMin);
        dest.writeValue(priceMax);
    }

    @Override public int describeContents() { return 0; }

    public static final Creator<Filters> CREATOR = new Creator<Filters>() {
        @Override public Filters createFromParcel(Parcel in) { return new Filters(in); }
        @Override public Filters[] newArray(int size) { return new Filters[size]; }
    };
}
