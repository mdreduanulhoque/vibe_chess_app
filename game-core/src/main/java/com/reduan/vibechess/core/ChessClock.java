package com.reduan.vibechess.core;

import java.util.Objects;

/**
 * Pure Java chess clock model with per-side remaining milliseconds, active side,
 * {@link #tick(long)} deduction, and flag detection when a side reaches 0.
 * <p>
 * Supports both direct mutation via {@link #tick(long)} / {@link #switchActiveSide()}
 * and immutable snapshots via {@link #withTick(long)} / {@link #withActiveSide(Color)} / {@link #copy()}.
 */
public final class ChessClock {

    public static final long ONE_MINUTE_MILLIS = 60_000L;
    public static final long TWO_MINUTES_MILLIS = 120_000L;

    private final long initialMillis;
    private long whiteRemainingMillis;
    private long blackRemainingMillis;
    private Color activeSide;

    public ChessClock(long initialMillis) {
        this(initialMillis, initialMillis, initialMillis, Color.WHITE);
    }

    public ChessClock(long initialMillis,
                      long whiteRemainingMillis,
                      long blackRemainingMillis,
                      Color activeSide) {
        if (initialMillis <= 0) {
            throw new IllegalArgumentException("initialMillis must be > 0");
        }
        this.initialMillis = initialMillis;
        this.whiteRemainingMillis = Math.max(0L, whiteRemainingMillis);
        this.blackRemainingMillis = Math.max(0L, blackRemainingMillis);
        this.activeSide = Objects.requireNonNull(activeSide, "activeSide must not be null");
    }

    /**
     * Advances the active side's clock by {@code elapsedMillis}.
     * If either side is already flagged (at 0), no further time is deducted.
     *
     * @param elapsedMillis non-negative elapsed time in milliseconds
     * @return true if a side flagged (reached 0) as a result of or prior to this tick
     */
    public boolean tick(long elapsedMillis) {
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("elapsedMillis must be >= 0");
        }
        if (isFlagged() || elapsedMillis == 0) {
            return isFlagged();
        }
        if (activeSide == Color.WHITE) {
            whiteRemainingMillis = Math.max(0L, whiteRemainingMillis - elapsedMillis);
        } else {
            blackRemainingMillis = Math.max(0L, blackRemainingMillis - elapsedMillis);
        }
        return isFlagged();
    }

    /**
     * Returns a new {@link ChessClock} after applying {@code elapsedMillis} to the active side.
     */
    public ChessClock withTick(long elapsedMillis) {
        ChessClock copy = copy();
        copy.tick(elapsedMillis);
        return copy;
    }

    /**
     * Switches the active clock to the specified side.
     */
    public void setActiveSide(Color side) {
        this.activeSide = Objects.requireNonNull(side, "side must not be null");
    }

    /**
     * Switches the active clock to the opposite side.
     */
    public void switchActiveSide() {
        this.activeSide = this.activeSide.opposite();
    }

    /**
     * Returns a new {@link ChessClock} with the active side set to {@code side}.
     */
    public ChessClock withActiveSide(Color side) {
        return new ChessClock(initialMillis, whiteRemainingMillis, blackRemainingMillis, side);
    }

    /**
     * Returns an independent copy of this clock.
     */
    public ChessClock copy() {
        return new ChessClock(initialMillis, whiteRemainingMillis, blackRemainingMillis, activeSide);
    }

    public long getInitialMillis() {
        return initialMillis;
    }

    public long getWhiteRemainingMillis() {
        return whiteRemainingMillis;
    }

    public long getBlackRemainingMillis() {
        return blackRemainingMillis;
    }

    public long getRemainingMillis(Color color) {
        Objects.requireNonNull(color, "color must not be null");
        return color == Color.WHITE ? whiteRemainingMillis : blackRemainingMillis;
    }

    public Color getActiveSide() {
        return activeSide;
    }

    /**
     * Returns true if ANY side's remaining time has reached 0.
     */
    public boolean isFlagged() {
        return whiteRemainingMillis <= 0L || blackRemainingMillis <= 0L;
    }

    /**
     * Returns true if the specified side's remaining time has reached 0.
     */
    public boolean isFlagged(Color color) {
        return getRemainingMillis(color) <= 0L;
    }

    /**
     * Returns the side whose clock reached 0, or {@code null} if neither side has flagged.
     */
    public Color getFlaggedSide() {
        if (whiteRemainingMillis <= 0L) {
            return Color.WHITE;
        }
        if (blackRemainingMillis <= 0L) {
            return Color.BLACK;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChessClock that)) return false;
        return initialMillis == that.initialMillis
                && whiteRemainingMillis == that.whiteRemainingMillis
                && blackRemainingMillis == that.blackRemainingMillis
                && activeSide == that.activeSide;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialMillis, whiteRemainingMillis, blackRemainingMillis, activeSide);
    }
}
