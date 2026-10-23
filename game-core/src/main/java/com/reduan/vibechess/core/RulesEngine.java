package com.reduan.vibechess.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pure state transition engine for Vibe Chess:
 * <ul>
 *   <li>{@link #applyMove(GameState, Move)} applies a move, updating pieces, castling rights,
 *       en passant target, captured pieces, and active turn/clock. Capturing a king removes it
 *       and its castling rights while the game continues.</li>
 *   <li>{@link #skipTurn(GameState)} skips the current side's turn when it has no legal moves,
 *       keeping its clock running.</li>
 *   <li>{@link #tick(GameState, long)} advances the running clock and ends the game immediately
 *       when any side's clock hits 0 (higher points wins, equal = draw).</li>
 * </ul>
 */
public final class RulesEngine {

    private RulesEngine() {
    }

    /**
     * Applies {@code move} to {@code state} and returns the new immutable {@link GameState}.
     */
    public static GameState applyMove(GameState state, Move move) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(move, "move must not be null");

        if (state.isGameOver()) {
            throw new IllegalStateException("Cannot apply move when game is already over");
        }

        Board board = state.getBoard();
        Piece movingPiece = board.getPieceAt(move.from());
        if (movingPiece == null) {
            throw new IllegalArgumentException("No piece at origin square: " + move.from());
        }
        Color mover = state.getSideToMove();
        if (movingPiece.color() != mover) {
            throw new IllegalArgumentException(
                    "Piece at " + move.from() + " belongs to " + movingPiece.color()
                            + ", but side to move is " + mover);
        }

        Piece[][] grid = board.copyGrid();
        List<Piece> capturedList = new ArrayList<>(state.getCapturedPieces());
        CastlingRights rights = state.getCastlingRights();

        // Remove piece from origin square
        grid[move.from().rank()][move.from().file()] = null;

        // Handle capture (standard or en passant)
        Piece capturedPiece;
        if (move.isEnPassant()) {
            int capturedRank = move.from().rank();
            int capturedFile = move.to().file();
            capturedPiece = grid[capturedRank][capturedFile];
            grid[capturedRank][capturedFile] = null;
        } else {
            capturedPiece = grid[move.to().rank()][move.to().file()];
        }

        if (capturedPiece != null) {
            capturedList.add(capturedPiece);
            // If an opponent's King is captured, revoke that opponent's castling rights; game continues!
            if (capturedPiece.type() == PieceType.KING) {
                rights = rights.withoutColor(capturedPiece.color());
            } else if (capturedPiece.type() == PieceType.ROOK) {
                rights = rights.withoutRookAt(move.to());
            }
        }

        // Place moved (or promoted) piece on destination square
        Piece placedPiece = move.isPromotion()
                ? Piece.of(mover, move.promotion())
                : movingPiece;
        grid[move.to().rank()][move.to().file()] = placedPiece;

        // Handle castling rook movement
        if (move.isCastle()) {
            int rank = move.from().rank();
            if (move.to().file() == 6) {
                // King-side castle: rook moves from h-file (7) to f-file (5)
                Piece rook = grid[rank][7];
                grid[rank][7] = null;
                grid[rank][5] = rook;
            } else if (move.to().file() == 2) {
                // Queen-side castle: rook moves from a-file (0) to d-file (3)
                Piece rook = grid[rank][0];
                grid[rank][0] = null;
                grid[rank][3] = rook;
            }
        }

        // Update castling rights for the moving piece
        if (movingPiece.type() == PieceType.KING) {
            rights = rights.withoutColor(mover);
        } else if (movingPiece.type() == PieceType.ROOK) {
            rights = rights.withoutRookAt(move.from());
        }

        // Compute new en-passant target square (only valid immediately on the next turn)
        Square nextEnPassantTarget = null;
        if (movingPiece.type() == PieceType.PAWN
                && Math.abs(move.to().rank() - move.from().rank()) == 2) {
            int epRank = (move.from().rank() + move.to().rank()) / 2;
            nextEnPassantTarget = Square.of(epRank, move.from().file());
        }

        Board nextBoard = Board.fromGridUnchecked(grid);
        Color nextSideToMove = mover.opposite();
        ChessClock nextClock = state.getClock().withActiveSide(nextSideToMove);

        return new GameState(
                nextBoard,
                nextSideToMove,
                rights,
                nextEnPassantTarget,
                nextClock,
                GameStatus.ACTIVE,
                GameResult.ONGOING,
                capturedList,
                move
        );
    }

    /**
     * Skips the turn of the side to move when it has no legal moves.
     * Per PROJECT_SPEC.md: "If a side has no legal move, its turn is skipped and its clock keeps running."
     */
    public static GameState skipTurn(GameState state) {
        Objects.requireNonNull(state, "state must not be null");
        if (state.isGameOver()) {
            throw new IllegalStateException("Cannot skip turn when game is already over");
        }
        Color skippedSide = state.getSideToMove();
        Color nextSideToMove = skippedSide.opposite();
        // Keep the skipped side's clock running as required by PROJECT_SPEC.md
        ChessClock nextClock = state.getClock().withActiveSide(skippedSide);

        return new GameState(
                state.getBoard(),
                nextSideToMove,
                state.getCastlingRights(),
                null,
                nextClock,
                GameStatus.ACTIVE,
                GameResult.ONGOING,
                state.getCapturedPieces(),
                state.getLastMove()
        );
    }

    /**
     * Advances the active clock by {@code elapsedMillis}.
     * When ANY side's clock reaches 0, the game ends immediately:
     * higher points on the board wins; equal points = draw.
     */
    public static GameState tick(GameState state, long elapsedMillis) {
        Objects.requireNonNull(state, "state must not be null");
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("elapsedMillis must be >= 0");
        }
        if (state.isGameOver()) {
            return state;
        }

        ChessClock nextClock = state.getClock().withTick(elapsedMillis);
        GameStatus nextStatus = GameStatus.ACTIVE;
        GameResult nextResult = GameResult.ONGOING;

        if (nextClock.isFlagged()) {
            nextStatus = GameStatus.GAME_OVER;
            int whitePoints = state.getBoard().points(Color.WHITE);
            int blackPoints = state.getBoard().points(Color.BLACK);
            nextResult = GameResult.fromPointsAtTimeout(whitePoints, blackPoints);
        }

        return new GameState(
                state.getBoard(),
                state.getSideToMove(),
                state.getCastlingRights(),
                state.getEnPassantTarget(),
                nextClock,
                nextStatus,
                nextResult,
                state.getCapturedPieces(),
                state.getLastMove()
        );
    }
}
