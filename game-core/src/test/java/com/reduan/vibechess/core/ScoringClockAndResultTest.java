package com.reduan.vibechess.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests point scoring after captures, chess clock ticking/flagging,
 * timeout game result resolution (win/loss/draw), and skipTurn when a side has no moves.
 */
public class ScoringClockAndResultTest {

    @Test
    public void initialBoardHas38PointsPerSideAndUpdatesAfterCaptures() {
        GameState state = GameState.initial();
        // 8*1 (P) + 2*2 (N) + 2*3 (B) + 2*4 (R) + 1*5 (Q) + 1*7 (K) = 38
        assertEquals(38, Scoring.points(state, Color.WHITE));
        assertEquals(38, Scoring.points(state, Color.BLACK));

        Board custom = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))   // 7
                .withPieceAt(Square.fromAlgebraic("d1"), Piece.of(Color.WHITE, PieceType.QUEEN))  // 5
                .withPieceAt(Square.fromAlgebraic("d5"), Piece.of(Color.BLACK, PieceType.ROOK))   // 4
                .withPieceAt(Square.fromAlgebraic("b3"), Piece.of(Color.BLACK, PieceType.BISHOP)) // 3
                .withPieceAt(Square.fromAlgebraic("f6"), Piece.of(Color.BLACK, PieceType.KNIGHT)) // 2
                .withPieceAt(Square.fromAlgebraic("a7"), Piece.of(Color.BLACK, PieceType.PAWN));  // 1

        GameState s = GameState.custom(
                custom, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));
        assertEquals(12, s.points(Color.WHITE));
        assertEquals(10, s.points(Color.BLACK));

        // White Queen captures Black Rook on d5 (-4 for Black)
        GameState afterCapture = s.applyMove(
                Move.capture(Square.fromAlgebraic("d1"), Square.fromAlgebraic("d5")));
        assertEquals(12, afterCapture.points(Color.WHITE));
        assertEquals(6, afterCapture.points(Color.BLACK));
        assertEquals(1, afterCapture.getCapturedPiecesOfColor(Color.BLACK).size());
    }

    @Test
    public void clockExpiryEndsGameImmediatelyAndHigherPointsWins() {
        Board whiteLeading = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("d1"), Piece.of(Color.WHITE, PieceType.QUEEN))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING));

        GameState state = GameState.custom(
                whiteLeading,
                Color.WHITE,
                CastlingRights.none(),
                null,
                new ChessClock(60_000L, 5_000L, 40_000L, Color.WHITE)
        );

        // Partial tick (3s): White has 2s left, game still ACTIVE
        GameState after3s = state.tick(3_000L);
        assertEquals(2_000L, after3s.getClock().getWhiteRemainingMillis());
        assertEquals(40_000L, after3s.getClock().getBlackRemainingMillis());
        assertFalse(after3s.isGameOver());
        assertEquals(GameResult.ONGOING, after3s.getResult());

        // Remaining tick (2s): White's clock hits 0 -> game over, White has 12 pts vs Black 7 pts -> WHITE_WINS
        GameState atZero = after3s.tick(2_000L);
        assertEquals(0L, atZero.getClock().getWhiteRemainingMillis());
        assertTrue(atZero.getClock().isFlagged());
        assertEquals(Color.WHITE, atZero.getClock().getFlaggedSide());
        assertTrue(atZero.isGameOver());
        assertEquals(GameStatus.GAME_OVER, atZero.getStatus());
        assertEquals(GameResult.WHITE_WINS, atZero.getResult());
        assertTrue(atZero.generateMoves().isEmpty());
    }

    @Test
    public void clockExpiryWhenBlackLeadsAndWhenEqualPointsIsDraw() {
        Board blackLeading = Board.empty()
                .withPieceAt(Square.fromAlgebraic("a2"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING));

        GameState blackWinState = GameState.custom(
                blackLeading,
                Color.BLACK,
                CastlingRights.none(),
                null,
                new ChessClock(60_000L, 30_000L, 1_000L, Color.BLACK)
        ).tick(1_500L);

        assertTrue(blackWinState.isGameOver());
        assertEquals(0L, blackWinState.getClock().getBlackRemainingMillis());
        assertEquals(GameResult.BLACK_WINS, blackWinState.getResult());

        // Equal points at timeout -> DRAW
        GameState drawState = GameState.initial(60_000L).tick(60_000L);
        assertTrue(drawState.isGameOver());
        assertEquals(GameResult.DRAW, drawState.getResult());
    }

    @Test
    public void skipTurnWhenSideHasNoMovesPassesTurnAndKeepsClockRunning() {
        // White has a single pawn at a3 blocked by Black's pawn at a4
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("a3"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("a4"), Piece.of(Color.BLACK, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("h7"), Piece.of(Color.BLACK, PieceType.PAWN));

        GameState state = GameState.custom(
                board,
                Color.WHITE,
                CastlingRights.none(),
                Square.fromAlgebraic("b6"),
                new ChessClock(60_000L, 50_000L, 50_000L, Color.WHITE)
        );

        assertFalse(state.hasLegalMoves());
        assertTrue(state.generateMoves().isEmpty());

        GameState skipped = state.skipTurn();
        assertEquals(Color.BLACK, skipped.getSideToMove());
        assertNull(skipped.getEnPassantTarget());
        assertEquals(GameStatus.ACTIVE, skipped.getStatus());
        assertEquals(Color.WHITE, skipped.getClock().getActiveSide());

        // Clock keeps running
        GameState ticked = skipped.tick(4_000L);
        assertEquals(46_000L, ticked.getClock().getWhiteRemainingMillis());
        assertTrue(ticked.hasLegalMoves());
    }

    @Test
    public void compactStringSerialization_roundTripsFullGameStateLosslessly() {
        // 1. Initial state
        GameState initial = GameState.initial(ChessClock.TWO_MINUTES_MILLIS);
        GameState restoredInitial = GameState.fromCompactString(initial.toCompactString());
        assertEquals(initial, restoredInitial);

        // 2. Mid-game state with double pawn push (en passant target), capture, and ticked clock
        GameState s1 = initial
                .applyMove(Move.quiet(Square.fromAlgebraic("e2"), Square.fromAlgebraic("e4")))
                .tick(1_250L);
        GameState restoredS1 = GameState.fromCompactString(s1.toCompactString());
        assertEquals(s1, restoredS1);
        assertEquals(Square.fromAlgebraic("e3"), restoredS1.getEnPassantTarget());

        GameState s2 = s1
                .applyMove(Move.quiet(Square.fromAlgebraic("d7"), Square.fromAlgebraic("d5")))
                .tick(980L)
                .applyMove(Move.capture(Square.fromAlgebraic("e4"), Square.fromAlgebraic("d5")));
        GameState restoredS2 = GameState.fromCompactString(s2.toCompactString());
        assertEquals(s2, restoredS2);
        assertEquals(1, restoredS2.getCapturedPieces().size());

        // 3. Promotion + custom clock + castling rights
        Board promoBoard = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("a7"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("b8"), Piece.of(Color.BLACK, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING));
        GameState promoState = GameState.custom(
                promoBoard,
                Color.WHITE,
                new CastlingRights(true, false, false, true),
                null,
                new ChessClock(60_000L, 12_345L, 45_678L, Color.WHITE)
        ).applyMove(Move.promotion(Square.fromAlgebraic("a7"), Square.fromAlgebraic("b8"), PieceType.QUEEN, true));
        GameState restoredPromo = GameState.fromCompactString(promoState.toCompactString());
        assertEquals(promoState, restoredPromo);

        // 4. Flagged game-over state
        GameState flagged = s2.tick(200_000L);
        assertTrue(flagged.isGameOver());
        GameState restoredFlagged = GameState.fromCompactString(flagged.toCompactString());
        assertEquals(flagged, restoredFlagged);

        // 5. Malformed inputs throw IllegalArgumentException
        for (String invalid : new String[]{null, "", "V2|bad"}) {
            boolean threw = false;
            try {
                GameState.fromCompactString(invalid);
            } catch (IllegalArgumentException expected) {
                threw = true;
            }
            assertTrue("Expected IllegalArgumentException for: " + invalid, threw);
        }
    }
}
