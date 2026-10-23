package com.reduan.vibechess.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Tests castling (including through attacked squares), en passant (and its immediate expiry),
 * pawn promotion (4 choices), and king capture continuing the game.
 */
public class SpecialMovesAndKingCaptureTest {

    @Test
    public void castlingAllowedForBothSidesEvenThroughAttackedSquares() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("a1"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("h1"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("a8"), Piece.of(Color.BLACK, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("h8"), Piece.of(Color.BLACK, PieceType.ROOK))
                // Enemy queen attacking e1, f1, d1
                .withPieceAt(Square.fromAlgebraic("e4"), Piece.of(Color.BLACK, PieceType.QUEEN));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.all(), null, new ChessClock(60_000L));

        List<Move> whiteKingMoves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("e1"));
        Move kingSideCastle = Move.castle(Square.fromAlgebraic("e1"), Square.fromAlgebraic("g1"));
        Move queenSideCastle = Move.castle(Square.fromAlgebraic("e1"), Square.fromAlgebraic("c1"));

        assertTrue(whiteKingMoves.contains(kingSideCastle));
        assertTrue(whiteKingMoves.contains(queenSideCastle));

        // Apply White king-side castle and verify King at g1, Rook at f1, and White castling rights revoked
        GameState afterWhiteCastle = state.applyMove(kingSideCastle);
        assertEquals(Piece.of(Color.WHITE, PieceType.KING),
                afterWhiteCastle.getBoard().getPieceAt(Square.fromAlgebraic("g1")));
        assertEquals(Piece.of(Color.WHITE, PieceType.ROOK),
                afterWhiteCastle.getBoard().getPieceAt(Square.fromAlgebraic("f1")));
        assertNull(afterWhiteCastle.getBoard().getPieceAt(Square.fromAlgebraic("e1")));
        assertNull(afterWhiteCastle.getBoard().getPieceAt(Square.fromAlgebraic("h1")));
        assertFalse(afterWhiteCastle.getCastlingRights().canCastleKingSide(Color.WHITE));
        assertFalse(afterWhiteCastle.getCastlingRights().canCastleQueenSide(Color.WHITE));
        assertTrue(afterWhiteCastle.getCastlingRights().canCastleQueenSide(Color.BLACK));

        // Now Black castles queen-side (e8 -> c8)
        Move blackQueenSideCastle = Move.castle(Square.fromAlgebraic("e8"), Square.fromAlgebraic("c8"));
        List<Move> blackKingMoves = MoveGenerator.generateMovesFrom(
                afterWhiteCastle, Square.fromAlgebraic("e8"));
        assertTrue(blackKingMoves.contains(blackQueenSideCastle));

        GameState afterBlackCastle = afterWhiteCastle.applyMove(blackQueenSideCastle);
        assertEquals(Piece.of(Color.BLACK, PieceType.KING),
                afterBlackCastle.getBoard().getPieceAt(Square.fromAlgebraic("c8")));
        assertEquals(Piece.of(Color.BLACK, PieceType.ROOK),
                afterBlackCastle.getBoard().getPieceAt(Square.fromAlgebraic("d8")));
        assertNull(afterBlackCastle.getBoard().getPieceAt(Square.fromAlgebraic("a8")));
    }

    @Test
    public void castlingBlockedWhenSquaresBetweenAreOccupied() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("a1"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("h1"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("b1"), Piece.of(Color.WHITE, PieceType.KNIGHT))
                .withPieceAt(Square.fromAlgebraic("f1"), Piece.of(Color.WHITE, PieceType.BISHOP));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.all(), null, new ChessClock(60_000L));

        List<Move> kingMoves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("e1"));
        assertFalse(kingMoves.contains(Move.castle(Square.fromAlgebraic("e1"), Square.fromAlgebraic("g1"))));
        assertFalse(kingMoves.contains(Move.castle(Square.fromAlgebraic("e1"), Square.fromAlgebraic("c1"))));
    }

    @Test
    public void enPassantValidImmediatelyAndExpiresNextTurn() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e5"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("a2"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("d7"), Piece.of(Color.BLACK, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("h7"), Piece.of(Color.BLACK, PieceType.PAWN));

        GameState state = GameState.custom(
                board, Color.BLACK, CastlingRights.none(), null, new ChessClock(60_000L));

        // Black plays d7-d5 (double pawn step)
        Move doubleStep = Move.quiet(Square.fromAlgebraic("d7"), Square.fromAlgebraic("d5"));
        GameState afterDoubleStep = state.applyMove(doubleStep);

        assertEquals(Square.fromAlgebraic("d6"), afterDoubleStep.getEnPassantTarget());

        // White can immediately capture en passant: e5xd6 e.p.
        Move epMove = Move.enPassant(Square.fromAlgebraic("e5"), Square.fromAlgebraic("d6"));
        List<Move> whitePawnMoves = MoveGenerator.generateMovesFrom(
                afterDoubleStep, Square.fromAlgebraic("e5"));
        assertTrue(whitePawnMoves.contains(epMove));

        // Applying en passant removes Black's pawn at d5 and places White's pawn at d6
        GameState afterEpCapture = afterDoubleStep.applyMove(epMove);
        assertEquals(Piece.of(Color.WHITE, PieceType.PAWN),
                afterEpCapture.getBoard().getPieceAt(Square.fromAlgebraic("d6")));
        assertNull(afterEpCapture.getBoard().getPieceAt(Square.fromAlgebraic("d5")));
        assertNull(afterEpCapture.getEnPassantTarget());

        // Branch 2: If White instead plays a2-a3, en passant expires
        GameState afterOtherWhiteMove = afterDoubleStep.applyMove(
                Move.quiet(Square.fromAlgebraic("a2"), Square.fromAlgebraic("a3")));
        assertNull(afterOtherWhiteMove.getEnPassantTarget());

        GameState backToWhite = afterOtherWhiteMove.applyMove(
                Move.quiet(Square.fromAlgebraic("h7"), Square.fromAlgebraic("h6")));
        List<Move> laterWhitePawnMoves = MoveGenerator.generateMovesFrom(
                backToWhite, Square.fromAlgebraic("e5"));
        assertFalse(laterWhitePawnMoves.contains(epMove));
    }

    @Test
    public void pawnPromotionGeneratesFourChoicesAndUpdatesPieceAndPoints() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("a7"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("b8"), Piece.of(Color.BLACK, PieceType.ROOK));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        // a7->a8 quiet promotions (4) + a7xb8 capture promotions (4) = 8 moves
        List<Move> promoMoves = MoveGenerator.generateMovesFrom(state, Square.fromAlgebraic("a7"));
        assertEquals(8, promoMoves.size());

        for (PieceType choice : PieceType.PROMOTION_CHOICES) {
            assertTrue(promoMoves.contains(
                    Move.promotion(Square.fromAlgebraic("a7"), Square.fromAlgebraic("a8"), choice, false)));
            assertTrue(promoMoves.contains(
                    Move.promotion(Square.fromAlgebraic("a7"), Square.fromAlgebraic("b8"), choice, true)));
        }

        // Apply capture promotion to Queen (a7xb8=Q)
        Move promoteToQueen = Move.promotion(
                Square.fromAlgebraic("a7"), Square.fromAlgebraic("b8"), PieceType.QUEEN, true);
        GameState afterPromo = state.applyMove(promoteToQueen);

        assertEquals(Piece.of(Color.WHITE, PieceType.QUEEN),
                afterPromo.getBoard().getPieceAt(Square.fromAlgebraic("b8")));
        assertEquals(5, afterPromo.points(Color.WHITE));
        assertEquals(0, afterPromo.points(Color.BLACK));
    }

    @Test
    public void capturingKingRemovesKingAndCastlingRightsAndGameContinues() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e4"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("a8"), Piece.of(Color.BLACK, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("h8"), Piece.of(Color.BLACK, PieceType.ROOK));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.all(), null, new ChessClock(60_000L));

        // White Rook captures Black King on e8
        Move captureKing = Move.capture(Square.fromAlgebraic("e4"), Square.fromAlgebraic("e8"));
        assertTrue(state.generateMoves().contains(captureKing));

        GameState afterKingCapture = state.applyMove(captureKing);

        // Black king is removed, Black castling rights are revoked, but game is still ACTIVE!
        assertFalse(afterKingCapture.getBoard().hasKing(Color.BLACK));
        assertFalse(afterKingCapture.getCastlingRights().canCastleKingSide(Color.BLACK));
        assertFalse(afterKingCapture.getCastlingRights().canCastleQueenSide(Color.BLACK));
        assertEquals(GameStatus.ACTIVE, afterKingCapture.getStatus());
        assertEquals(GameResult.ONGOING, afterKingCapture.getResult());

        // Black can still move its remaining pieces (e.g., a8xe8 capturing the White Rook)
        Move blackRookRecapture = Move.capture(Square.fromAlgebraic("a8"), Square.fromAlgebraic("e8"));
        assertTrue(afterKingCapture.generateMoves().contains(blackRookRecapture));
        GameState afterBlackMove = afterKingCapture.applyMove(blackRookRecapture);
        assertNotNull(afterBlackMove);
        assertEquals(GameStatus.ACTIVE, afterBlackMove.getStatus());
    }
}
