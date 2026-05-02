package com.example.androidapp.data.model;

import com.google.gson.annotations.SerializedName;

public class BookingsSummary {

    @SerializedName("summary")
    private SummaryData summary;

    public SummaryData getSummary() {
        return summary;
    }

    public static class SummaryData {
        @SerializedName("totalBookings")
        private int totalBookings;

        @SerializedName("upcomingBookings")
        private int upcomingBookings;

        @SerializedName("finalizedBookings")
        private int finalizedBookings;

        public int getTotalBookings() {
            return totalBookings;
        }

        public int getUpcomingBookings() {
            return upcomingBookings;
        }

        public int getFinalizedBookings() {
            return finalizedBookings;
        }
    }
}
