package com.reduan.vibechess;

import com.reduan.vibechess.ui.game.ClockFormatter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ClockFormatter} verifying mm:ss and tenths-of-a-second formatting under 10s.
 */
public class ClockFormatterTest {

    @Test
    public void formatsMinutesAndSecondsWhenAtLeastTenSeconds() {
        assertEquals("02:00", ClockFormatter.formatMillis(120_000L));
        assertEquals("01:00", ClockFormatter.formatMillis(60_000L));
        assertEquals("00:59", ClockFormatter.formatMillis(59_900L));
        assertEquals("00:10", ClockFormatter.formatMillis(10_000L));
    }

    @Test
    public void formatsWithTenthsUnderTenSeconds() {
        assertEquals("00:09.9", ClockFormatter.formatMillis(9_950L));
        assertEquals("00:09.8", ClockFormatter.formatMillis(9_800L));
        assertEquals("00:05.3", ClockFormatter.formatMillis(5_320L));
        assertEquals("00:00.1", ClockFormatter.formatMillis(100L));
        assertEquals("00:00.0", ClockFormatter.formatMillis(0L));
        assertEquals("00:00.0", ClockFormatter.formatMillis(-500L));
    }
}
