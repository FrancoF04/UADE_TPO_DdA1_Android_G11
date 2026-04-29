package com.example.androidapp.data.local;

import com.example.androidapp.util.DateTimeUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SpotAdjustmentManager {
    private static SpotAdjustmentManager instance;
    private final Map<String, Integer> adjustments = new HashMap<>();

    private SpotAdjustmentManager() {
    }

    public static synchronized SpotAdjustmentManager getInstance() {
        if (instance == null) {
            instance = new SpotAdjustmentManager();
        }
        return instance;
    }

    public synchronized void adjustSpots(String activityId, String scheduleDate, int delta) {
        String key = buildKey(activityId, scheduleDate);
        if (key == null) {
            return;
        }

        int current = adjustments.getOrDefault(key, 0);
        int updated = current + delta;
        if (updated == 0) {
            adjustments.remove(key);
            return;
        }

        adjustments.put(key, updated);
    }

    public synchronized int getAdjustedSpots(String activityId, String scheduleDate, int apiSpots) {
        String key = buildKey(activityId, scheduleDate);
        if (key == null) {
            return Math.max(0, apiSpots);
        }

        int delta = adjustments.getOrDefault(key, 0);
        return Math.max(0, apiSpots + delta);
    }

    private String buildKey(String activityId, String scheduleDate) {
        if (activityId == null || activityId.trim().isEmpty() || scheduleDate == null || scheduleDate.trim().isEmpty()) {
            return null;
        }

        Instant instant = DateTimeUtils.parseToInstant(scheduleDate);
        String normalizedDate = instant != null ? instant.toString() : scheduleDate.trim();
        return activityId.trim() + "|" + normalizedDate;
    }
}