package com.reduan.vibechess.ui.game;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Formats chess clock remaining milliseconds for display:
 * <ul>
 *   <li>10 seconds or more ({@code >= 10_000 ms}): {@code mm:ss} (e.g. {@code 01:00}, {@code 00:10}).</li>
 *   <li>Under 10 seconds ({@code < 10_000 ms}): {@code 00:0s.t} with tenths of a second
 *       (e.g. {@code 00:09.8}, {@code 00:00.0}).</li>
 * </ul>
 */
public final class ClockFormatter {

    public static final long TENTHS_THRESHOLD_MILLIS = 10_000L;

    private ClockFormatter() {
    }

    @NonNull
    public static String formatMillis(long remainingMillis) {
        long clamped = Math.max(0L, remainingMillis);
        if (clamped < TENTHS_THRESHOLD_MILLIS) {
            long totalTenths = clamped / 100L;
            long seconds = totalTenths / 10L;
            long tenths = totalTenths % 10L;
            return String.format(Locale.US, "00:%02d.%d", seconds, tenths);
        }

        // For >= 10 seconds, ceil to whole display seconds or floor?
        // Using (clamped + 999) / 1000 or clamped / 1000:
        // At exact 60_000ms -> 01:00; at 59_100ms -> 00:59; at 10_000ms -> 00:10.
        long totalSeconds = clamped / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
