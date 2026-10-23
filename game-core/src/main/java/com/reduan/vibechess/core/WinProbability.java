package com.reduan.vibechess.core;

import java.util.Objects;

/**
 * Computes whole-number winning probability percentages for White and Black
 * according to the Vibe Chess specification:
 * <pre>
 *   d = whitePoints - blackPoints
 *   r = (whiteMillis + blackMillis) / (2 * initialMillis)   // 1.0 at start, 0 at end
 *   k = 1.5 + 4.5 * r
 *   pWhite = 1 / (1 + exp(-d / k))
 * </pre>
 * Returns whole-number percentages that sum to 100.
 * When the game is over (any clock <= 0), returns 100/0, 0/100, or 50/50 on a tie.
 */
public final class WinProbability {

    private final int whitePercent;
    private final int blackPercent;

    public WinProbability(int whitePercent, int blackPercent) {
        if (whitePercent < 0 || whitePercent > 100 || blackPercent < 0 || blackPercent > 100
                || whitePercent + blackPercent != 100) {
            throw new IllegalArgumentException(
                    "Invalid percentages: white=" + whitePercent + ", black=" + blackPercent);
        }
        this.whitePercent = whitePercent;
        this.blackPercent = blackPercent;
    }

    /**
     * Computes the winning probability percentages from points and clock values.
     */
    public static WinProbability compute(int whitePoints,
                                         int blackPoints,
                                         long whiteMillis,
                                         long blackMillis,
                                         long initialMillis) {
        if (initialMillis <= 0) {
            throw new IllegalArgumentException("initialMillis must be > 0");
        }

        // Game over when any side's clock reaches 0
        if (whiteMillis <= 0L || blackMillis <= 0L) {
            if (whitePoints > blackPoints) {
                return new WinProbability(100, 0);
            } else if (blackPoints > whitePoints) {
                return new WinProbability(0, 100);
            } else {
                return new WinProbability(50, 50);
            }
        }

        double d = (double) (whitePoints - blackPoints);
        double r = ((double) (whiteMillis + blackMillis)) / (2.0 * (double) initialMillis);
        r = Math.max(0.0, Math.min(1.0, r));
        double k = 1.5 + 4.5 * r;
        double pWhite = 1.0 / (1.0 + Math.exp(-d / k));

        int whitePct = (int) Math.round(pWhite * 100.0);
        whitePct = Math.max(0, Math.min(100, whitePct));
        int blackPct = 100 - whitePct;

        return new WinProbability(whitePct, blackPct);
    }

    /**
     * Convenience overload computing winning probabilities directly from a {@link GameState}.
     */
    public static WinProbability compute(GameState state) {
        Objects.requireNonNull(state, "state must not be null");
        int whitePoints = state.points(Color.WHITE);
        int blackPoints = state.points(Color.BLACK);
        ChessClock clock = state.getClock();
        if (state.isGameOver()) {
            return compute(whitePoints, blackPoints, 0L, 0L, clock.getInitialMillis());
        }
        return compute(
                whitePoints,
                blackPoints,
                clock.getWhiteRemainingMillis(),
                clock.getBlackRemainingMillis(),
                clock.getInitialMillis()
        );
    }

    public int getWhitePercent() {
        return whitePercent;
    }

    public int whitePercent() {
        return whitePercent;
    }

    public int getBlackPercent() {
        return blackPercent;
    }

    public int blackPercent() {
        return blackPercent;
    }

    public int[] toArray() {
        return new int[]{whitePercent, blackPercent};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WinProbability that)) return false;
        return whitePercent == that.whitePercent && blackPercent == that.blackPercent;
    }

    @Override
    public int hashCode() {
        return 31 * whitePercent + blackPercent;
    }

    @Override
    public String toString() {
        return "WinProbability{white=" + whitePercent + "%, black=" + blackPercent + "%}";
    }
}
