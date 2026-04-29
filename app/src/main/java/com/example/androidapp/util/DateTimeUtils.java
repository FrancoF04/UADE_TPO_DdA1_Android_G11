package com.example.androidapp.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateTimeUtils {
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})");

    private DateTimeUtils() {
    }

    public static Instant parseToInstant(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (RuntimeException ignored) {
        }

        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (RuntimeException ignored) {
        }

        try {
            return LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant();
        } catch (RuntimeException ignored) {
        }

        try {
            return LocalDate.parse(raw).atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean isFutureOrNow(String raw) {
        Instant instant = parseToInstant(raw);
        return instant != null && !instant.isBefore(Instant.now());
    }

    public static String extractDateKey(String rawDate) {
        if (rawDate == null) {
            return null;
        }

        Matcher matcher = DATE_PATTERN.matcher(rawDate);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return rawDate;
    }

    public static String extractTimePart(String rawDate) {
        if (rawDate == null) {
            return "--";
        }

        Matcher matcher = TIME_PATTERN.matcher(rawDate);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "--";
    }
}
