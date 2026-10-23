package com.reduan.vibechess.core;

import java.util.Objects;

/**
 * {@link MoveSource} implementation for a local human player tapping moves on the device.
 */
public final class LocalHumanMoveSource implements MoveSource {

    private GameState activeState;
    private MoveCallback pendingCallback;

    @Override
    public boolean isHumanInteractive() {
        return true;
    }

    @Override
    public void requestMove(GameState state, MoveCallback callback) {
        this.activeState = Objects.requireNonNull(state, "state must not be null");
        this.pendingCallback = Objects.requireNonNull(callback, "callback must not be null");
    }

    /**
     * Delivers a move selected by the human player on the UI board.
     *
     * @param move the move selected from the current legal move list
     * @return true if this source was awaiting a move and accepted it; false otherwise
     */
    public boolean submitMove(Move move) {
        Objects.requireNonNull(move, "move must not be null");
        if (pendingCallback == null || activeState == null) {
            return false;
        }
        MoveCallback callback = this.pendingCallback;
        this.pendingCallback = null;
        this.activeState = null;
        callback.onMoveSelected(move);
        return true;
    }

    /**
     * Returns true if this source is currently waiting for the human player to pick a move.
     */
    public boolean isAwaitingMove() {
        return pendingCallback != null;
    }

    /**
     * Returns the {@link GameState} for which a move is currently awaited, or null if idle.
     */
    public GameState getActiveState() {
        return activeState;
    }

    @Override
    public void cancel() {
        this.pendingCallback = null;
        this.activeState = null;
    }
}
