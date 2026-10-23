package com.reduan.vibechess.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Tests perft-style move counts from the initial position and individual piece movement rules
 * (including no-check king movement into attacked squares).
 */
public class PerftAndPieceMovementTest {

    private long perft(GameState state, int depth) {
        if (depth == 0) {
            return 1L;
        }
        List<Move> moves = MoveGenerator.generateMoves(state);
        if (depth == 1) {
            return moves.size();
        }
        long nodes = 0L;
        for (Move move : moves) {
            nodes += perft(state.applyMove(move), depth - 1);
        }
        return nodes;
    }

    @Test
    public void perftDepth1FromStartPositionIs20() {
        GameState initial = GameState.initial();
        assertEquals(20L, perft(initial, 1));
    }

    @Test
    public void perftDepth2FromStartPositionIs400() {
        GameState initial = GameState.initial();
        assertEquals(400L, perft(initial, 2));
    }

    @Test
    public void perftDepth3FromStartPositionIs8902() {
        GameState initial = GameState.initial();
        assertEquals(8902L, perft(initial, 3));
    }

    @Test
    public void pawnMovementSingleDoubleBlockedAndCapture() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e2"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("d3"), Piece.of(Color.BLACK, PieceType.KNIGHT))
                .withPieceAt(Square.fromAlgebraic("f3"), Piece.of(Color.WHITE, PieceType.BISHOP))
                .withPieceAt(Square.fromAlgebraic("a2"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("a3"), Piece.of(Color.BLACK, PieceType.PAWN));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        // e2 pawn can move to e3, e4, and capture d3 (f3 has friendly piece)
        List<Move> e2Moves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("e2"));
        assertEquals(3, e2Moves.size());
        assertTrue(e2Moves.contains(Move.quiet(Square.fromAlgebraic("e2"), Square.fromAlgebraic("e3"))));
        assertTrue(e2Moves.contains(Move.quiet(Square.fromAlgebraic("e2"), Square.fromAlgebraic("e4"))));
        assertTrue(e2Moves.contains(Move.capture(Square.fromAlgebraic("e2"), Square.fromAlgebraic("d3"))));

        // a2 pawn is blocked by a3 pawn, so 0 moves
        List<Move> a2Moves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("a2"));
        assertTrue(a2Moves.isEmpty());
    }

    @Test
    public void knightMovementInCenterAndCorner() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("d4"), Piece.of(Color.WHITE, PieceType.KNIGHT))
                .withPieceAt(Square.fromAlgebraic("a1"), Piece.of(Color.WHITE, PieceType.KNIGHT))
                .withPieceAt(Square.fromAlgebraic("c6"), Piece.of(Color.BLACK, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("e6"), Piece.of(Color.WHITE, PieceType.PAWN));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        // d4 knight has 8 targets, minus e6 (occupied by friendly) = 7 moves (including capture on c6)
        List<Move> d4Moves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("d4"));
        assertEquals(7, d4Moves.size());
        assertTrue(d4Moves.contains(Move.capture(Square.fromAlgebraic("d4"), Square.fromAlgebraic("c6"))));
        assertFalse(d4Moves.contains(Move.quiet(Square.fromAlgebraic("d4"), Square.fromAlgebraic("e6"))));

        // a1 corner knight has 2 targets (b3, c2)
        List<Move> a1Moves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("a1"));
        assertEquals(2, a1Moves.size());
    }

    @Test
    public void bishopSlidingAndBlocking() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("c1"), Piece.of(Color.WHITE, PieceType.BISHOP))
                .withPieceAt(Square.fromAlgebraic("f4"), Piece.of(Color.BLACK, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("b2"), Piece.of(Color.WHITE, PieceType.PAWN));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        // b2 blocks upper-left; upper-right goes d2, e3, and captures f4 (cannot reach g5)
        List<Move> moves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("c1"));
        assertEquals(3, moves.size());
        assertTrue(moves.contains(Move.quiet(Square.fromAlgebraic("c1"), Square.fromAlgebraic("d2"))));
        assertTrue(moves.contains(Move.quiet(Square.fromAlgebraic("c1"), Square.fromAlgebraic("e3"))));
        assertTrue(moves.contains(Move.capture(Square.fromAlgebraic("c1"), Square.fromAlgebraic("f4"))));
    }

    @Test
    public void rookSlidingAndBlocking() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("d4"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("d7"), Piece.of(Color.BLACK, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("d2"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("b4"), Piece.of(Color.WHITE, PieceType.KNIGHT));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        // Up: d5, d6, capture d7 (3)
        // Down: d3 (1, blocked at d2)
        // Left: c4 (1, blocked at b4)
        // Right: e4, f4, g4, h4 (4)
        // Total = 9 moves
        List<Move> moves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("d4"));
        assertEquals(9, moves.size());
        assertTrue(moves.contains(Move.capture(Square.fromAlgebraic("d4"), Square.fromAlgebraic("d7"))));
    }

    @Test
    public void queenCombinesOrthogonalAndDiagonalMoves() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("d4"), Piece.of(Color.WHITE, PieceType.QUEEN));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        // On an empty board at d4: 14 rook moves + 13 bishop moves = 27 moves
        List<Move> moves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("d4"));
        assertEquals(27, moves.size());
    }

    @Test
    public void kingCanMoveIntoAttackedSquaresBecauseThereIsNoCheck() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("d8"), Piece.of(Color.BLACK, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("f8"), Piece.of(Color.BLACK, PieceType.ROOK));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        // Every square around e1 (d1, d2, e2, f2, f1) is attacked by Black rooks,
        // yet all 5 moves are generated because Vibe Chess has NO check filtering.
        List<Move> kingMoves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("e1"));
        assertEquals(5, kingMoves.size());
    }
}
