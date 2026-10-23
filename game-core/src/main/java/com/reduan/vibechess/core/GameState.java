package com.reduan.vibechess.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a Vibe Chess game state:
 * board, side to move, castling rights, en-passant target square, chess clock,
 * game status, game result, captured pieces, and last move played.
 */
public final class GameState {

    private final Board board;
    private final Color sideToMove;
    private final CastlingRights castlingRights;
    private final Square enPassantTarget;
    private final ChessClock clock;
    private final GameStatus status;
    private final GameResult result;
    private final List<Piece> capturedPieces;
    private final Move lastMove;

    public GameState(Board board,
                     Color sideToMove,
                     CastlingRights castlingRights,
                     Square enPassantTarget,
                     ChessClock clock,
                     GameStatus status,
                     GameResult result,
                     List<Piece> capturedPieces,
                     Move lastMove) {
        this.board = Objects.requireNonNull(board, "board must not be null");
        this.sideToMove = Objects.requireNonNull(sideToMove, "sideToMove must not be null");
        this.castlingRights = Objects.requireNonNull(castlingRights, "castlingRights must not be null");
        this.enPassantTarget = enPassantTarget;
        this.clock = Objects.requireNonNull(clock, "clock must not be null").copy();
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.capturedPieces = capturedPieces == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(capturedPieces));
        this.lastMove = lastMove;
    }

    /**
     * Creates a new standard game state with the given initial clock time per side in milliseconds.
     * White moves first.
     */
    public static GameState initial(long initialClockMillis) {
        ChessClock clock = new ChessClock(initialClockMillis);
        return new GameState(
                Board.initial(),
                Color.WHITE,
                CastlingRights.all(),
                null,
                clock,
                GameStatus.ACTIVE,
                GameResult.ONGOING,
                List.of(),
                null
        );
    }

    /**
     * Creates a new standard game state with 1 minute (60,000 ms) per side.
     */
    public static GameState initial() {
        return initial(ChessClock.ONE_MINUTE_MILLIS);
    }

    /**
     * Creates a custom active game state (useful for tests and custom board setups).
     */
    public static GameState custom(Board board,
                                   Color sideToMove,
                                   CastlingRights castlingRights,
                                   Square enPassantTarget,
                                   ChessClock clock) {
        GameStatus status = clock.isFlagged() ? GameStatus.GAME_OVER : GameStatus.ACTIVE;
        GameResult result = clock.isFlagged()
                ? GameResult.fromPointsAtTimeout(board.points(Color.WHITE), board.points(Color.BLACK))
                : GameResult.ONGOING;
        return new GameState(
                board,
                sideToMove,
                castlingRights,
                enPassantTarget,
                clock,
                status,
                result,
                List.of(),
                null
        );
    }

    public Board getBoard() {
        return board;
    }

    public Board board() {
        return board;
    }

    public Color getSideToMove() {
        return sideToMove;
    }

    public Color sideToMove() {
        return sideToMove;
    }

    public CastlingRights getCastlingRights() {
        return castlingRights;
    }

    public CastlingRights castlingRights() {
        return castlingRights;
    }

    public Square getEnPassantTarget() {
        return enPassantTarget;
    }

    public Square enPassantTarget() {
        return enPassantTarget;
    }

    /**
     * Returns a defensive copy of the chess clock.
     */
    public ChessClock getClock() {
        return clock.copy();
    }

    public ChessClock clock() {
        return clock.copy();
    }

    public GameStatus getStatus() {
        return status;
    }

    public GameStatus status() {
        return status;
    }

    public GameResult getResult() {
        return result;
    }

    public GameResult result() {
        return result;
    }

    public boolean isGameOver() {
        return status.isGameOver();
    }

    public List<Piece> getCapturedPieces() {
        return capturedPieces;
    }

    /**
     * Returns all captured pieces that belonged to the specified {@code pieceColor}.
     */
    public List<Piece> getCapturedPiecesOfColor(Color pieceColor) {
        Objects.requireNonNull(pieceColor, "pieceColor must not be null");
        List<Piece> filtered = new ArrayList<>();
        for (Piece p : capturedPieces) {
            if (p.color() == pieceColor) {
                filtered.add(p);
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    public Move getLastMove() {
        return lastMove;
    }

    /**
     * Returns the current board point score for {@code color}.
     */
    public int points(Color color) {
        return board.points(color);
    }

    /**
     * Generates all pseudo-legal moves for the current {@code sideToMove}.
     */
    public List<Move> generateMoves() {
        return MoveGenerator.generateMoves(this);
    }

    /**
     * Returns true if the current {@code sideToMove} has at least one move.
     */
    public boolean hasLegalMoves() {
        return !MoveGenerator.generateMoves(this).isEmpty();
    }

    /**
     * Applies {@code move} and returns the resulting {@link GameState}.
     */
    public GameState applyMove(Move move) {
        return RulesEngine.applyMove(this, move);
    }

    /**
     * Skips the current side's turn when it has no legal moves, keeping its clock running.
     */
    public GameState skipTurn() {
        return RulesEngine.skipTurn(this);
    }

    /**
     * Advances the active clock by {@code elapsedMillis} and evaluates clock-expiry game over.
     */
    public GameState tick(long elapsedMillis) {
        return RulesEngine.tick(this, elapsedMillis);
    }
}
