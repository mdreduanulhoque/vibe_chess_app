package com.reduan.vibechess.core;

/**
 * Outcome of a Vibe Chess game.
 * <p>
 * Per PROJECT_SPEC.md: When ANY side's time reaches 0, the game ends immediately.
 * The side with more points on the board wins. Equal points = draw.
 */
public enum GameResult {
    ONGOING,
    WHITE_WINS,
    BLACK_WINS,
    DRAW;

    /**
     * Computes the game result from the two sides' point totals when a clock hits 0.
     */
    public static GameResult fromPointsAtTimeout(int whitePoints, int blackPoints) {
        if (whitePoints > blackPoints) {
            return WHITE_WINS;
        } else if (blackPoints > whitePoints) {
            return BLACK_WINS;
        } else {
            return DRAW;
        }
    }
}
