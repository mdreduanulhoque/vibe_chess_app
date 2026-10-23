package com.reduan.vibechess.core;

/**
 * Represents a player side / piece color in Vibe Chess.
 */
public enum Color {
    WHITE,
    BLACK;

    /**
     * Returns the opposing side.
     */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
