package com.example.androidapp.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateTimeUtils {
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}:\\d{2})");
    public static final ZoneId ARGENTINA_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_PART_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
            return LocalDateTime.parse(raw).atZone(ARGENTINA_ZONE).toInstant();
        } catch (RuntimeException ignored) {
        }

        try {
            return LocalDate.parse(raw).atStartOfDay(ARGENTINA_ZONE).toInstant();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean isFutureOrNow(String raw) {
        if (raw == null || raw.trim().isEmpty()) return false;
        Instant instant = parseToInstant(raw);
        if (instant == null) return false;
        LocalDate activityDate = instant.atZone(ARGENTINA_ZONE).toLocalDate();
        LocalDate today = LocalDate.now(ARGENTINA_ZONE);
        return !activityDate.isBefore(today);
    }

    public static boolean isToday(String raw) {
        if (raw == null || raw.trim().isEmpty()) return false;
        Instant instant = parseToInstant(raw);
        if (instant == null) return false;
        LocalDate activityDate = instant.atZone(ARGENTINA_ZONE).toLocalDate();
        LocalDate today = LocalDate.now(ARGENTINA_ZONE);
        return activityDate.isEqual(today);
    }

    public static String extractDateKey(String rawDate) {
        if (rawDate == null) {
            return null;
        }

        Instant instant = parseToInstant(rawDate);
        if (instant != null) {
            return instant.atZone(ARGENTINA_ZONE).format(DATE_KEY_FORMATTER);
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

        Instant instant = parseToInstant(rawDate);
        if (instant != null) {
            return instant.atZone(ARGENTINA_ZONE).format(TIME_PART_FORMATTER);
        }

        Matcher matcher = TIME_PATTERN.matcher(rawDate);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "--";
    }

    public static String formatFriendly(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        Instant instant = parseToInstant(raw);
        if (instant == null) {
            return raw;
        }
        try {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ARGENTINA_ZONE);
            return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return raw;
        }
    }
}
