package com.example.androidapp.util;

/**
 * Helper puro de countdown del boton "Reenviar codigo".
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
