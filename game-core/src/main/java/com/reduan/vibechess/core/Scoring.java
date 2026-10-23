package com.reduan.vibechess.core;

import java.util.Objects;

/**
 * Scoring utility for Vibe Chess.
 * Computes a side's points as the sum of its pieces currently on the board:
 * Pawn=1, Knight=2, Bishop=3, Rook=4, Queen=5, King=7.
 */
public final class Scoring {

    private Scoring() {
    }

    /**
     * Returns the total point score for {@code color} on {@code board}.
     */
    public static int points(Board board, Color color) {
        Objects.requireNonNull(board, "board must not be null");
        Objects.requireNonNull(color, "color must not be null");
        return board.points(color);
    }

    /**
     * Returns the total point score for {@code color} in {@code state}.
     */
    public static int points(GameState state, Color color) {
        Objects.requireNonNull(state, "state must not be null");
        return state.points(color);
    }
}
