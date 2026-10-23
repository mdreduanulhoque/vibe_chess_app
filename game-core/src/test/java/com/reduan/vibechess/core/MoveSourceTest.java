package com.reduan.vibechess.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MoveSource} and {@link LocalHumanMoveSource} alternating turns.
 */
public class MoveSourceTest {

    @Test
    public void localHumanMoveSource_acceptsMoveWhenRequestedAndClearsPendingState() {
        LocalHumanMoveSource source = new LocalHumanMoveSource();
        assertTrue(source.isHumanInteractive());
        assertFalse(source.isAwaitingMove());
        assertNull(source.getActiveState());

        GameState initial = GameState.initial();
        Move e2e4 = Move.quiet(Square.fromAlgebraic("e2"), Square.fromAlgebraic("e4"));

        // Submitting before requestMove returns false
        assertFalse(source.submitMove(e2e4));

        AtomicReference<Move> delivered = new AtomicReference<>();
        source.requestMove(initial, delivered::set);

        assertTrue(source.isAwaitingMove());
        assertEquals(initial, source.getActiveState());

        assertTrue(source.submitMove(e2e4));
        assertEquals(e2e4, delivered.get());
        assertFalse(source.isAwaitingMove());
        assertNull(source.getActiveState());

        // Duplicate submit after delivery is ignored
        assertFalse(source.submitMove(e2e4));
    }

    @Test
    public void localHumanMoveSources_alternateTurnsBetweenWhiteAndBlack() {
        LocalHumanMoveSource whiteSource = new LocalHumanMoveSource();
        LocalHumanMoveSource blackSource = new LocalHumanMoveSource();

        AtomicReference<GameState> stateRef = new AtomicReference<>(GameState.initial());

        // Request White's move
        assertEquals(Color.WHITE, stateRef.get().getSideToMove());
        whiteSource.requestMove(stateRef.get(), move -> {
            GameState afterWhite = stateRef.get().applyMove(move);
            stateRef.set(afterWhite);
            blackSource.requestMove(afterWhite, blackMove -> {
                GameState afterBlack = stateRef.get().applyMove(blackMove);
                stateRef.set(afterBlack);
            });
        });

        Move e2e4 = Move.quiet(Square.fromAlgebraic("e2"), Square.fromAlgebraic("e4"));
        assertTrue(whiteSource.submitMove(e2e4));
        assertEquals(Color.BLACK, stateRef.get().getSideToMove());
        assertEquals(e2e4, stateRef.get().getLastMove());
        assertTrue(blackSource.isAwaitingMove());

        Move e7e5 = Move.quiet(Square.fromAlgebraic("e7"), Square.fromAlgebraic("e5"));
        assertTrue(blackSource.submitMove(e7e5));
        assertEquals(Color.WHITE, stateRef.get().getSideToMove());
        assertEquals(e7e5, stateRef.get().getLastMove());
    }

    @Test
    public void cancel_clearsPendingRequest() {
        LocalHumanMoveSource source = new LocalHumanMoveSource();
        GameState initial = GameState.initial();
        AtomicReference<Move> delivered = new AtomicReference<>();

        source.requestMove(initial, delivered::set);
        assertTrue(source.isAwaitingMove());

        source.cancel();
        assertFalse(source.isAwaitingMove());
        assertFalse(source.submitMove(Move.quiet(Square.fromAlgebraic("e2"), Square.fromAlgebraic("e4"))));
        assertNull(delivered.get());
    }
}
