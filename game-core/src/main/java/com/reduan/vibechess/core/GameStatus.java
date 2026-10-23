package com.reduan.vibechess.core;

/**
 * Lifecycle status of a Vibe Chess game.
 */
public enum GameStatus {
    /**
     * The game is currently in progress (both clocks > 0).
     */
    ACTIVE,

    /**
     * A side's clock has reached 0 and the game has ended immediately.
     */
    GAME_OVER;

    public boolean isGameOver() {
        return this == GAME_OVER;
    }
}
