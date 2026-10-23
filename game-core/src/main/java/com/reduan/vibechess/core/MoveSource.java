package com.reduan.vibechess.core;

/**
 * Abstraction for a source of moves (local human tap input, AI engine, or remote network player)
 * so the game controller does not care where a move originates.
 */
public interface MoveSource {

    /**
     * Callback invoked when a move source selects a legal move.
     */
    @FunctionalInterface
    interface MoveCallback {
        void onMoveSelected(Move move);
    }

    /**
     * Returns true if this move source expects interactive local board taps from the UI.
     */
    boolean isHumanInteractive();

    /**
     * Requests a move for the active side in {@code state}.
     * When the move is decided, {@code callback.onMoveSelected(move)} is invoked.
     */
    void requestMove(GameState state, MoveCallback callback);

    /**
     * Cancels any pending move request.
     */
    default void cancel() {
    }
}
