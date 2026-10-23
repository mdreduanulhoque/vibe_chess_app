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

    /**
     * Serializes this {@link GameState} into a compact, deterministic single-line string
     * suitable for process-death state restoration or network/persistence snapshots.
     */
    public String toCompactString() {
        StringBuilder sb = new StringBuilder(160);
        sb.append("v1|");

        // 1. 64-square board (rank 0..7, file 0..7)
        for (int r = 0; r < Square.BOARD_SIZE; r++) {
            for (int f = 0; f < Square.BOARD_SIZE; f++) {
                Piece p = board.getPieceAt(r, f);
                sb.append(encodePieceChar(p));
            }
        }
        sb.append('|');

        // 2. Side to move
        sb.append(sideToMove.name()).append('|');

        // 3. Castling rights
        sb.append(castlingRights.canCastleKingSide(Color.WHITE) ? 'K' : '-');
        sb.append(castlingRights.canCastleQueenSide(Color.WHITE) ? 'Q' : '-');
        sb.append(castlingRights.canCastleKingSide(Color.BLACK) ? 'k' : '-');
        sb.append(castlingRights.canCastleQueenSide(Color.BLACK) ? 'q' : '-');
        sb.append('|');

        // 4. En-passant target square
        sb.append(enPassantTarget != null ? enPassantTarget.toAlgebraic() : "-").append('|');

        // 5. Clock
        sb.append(clock.getInitialMillis()).append(',')
                .append(clock.getWhiteRemainingMillis()).append(',')
                .append(clock.getBlackRemainingMillis()).append(',')
                .append(clock.getActiveSide().name()).append('|');

        // 6. Status & Result
        sb.append(status.name()).append('|');
        sb.append(result.name()).append('|');

        // 7. Captured pieces
        if (capturedPieces.isEmpty()) {
            sb.append('-');
        } else {
            for (int i = 0, n = capturedPieces.size(); i < n; i++) {
                sb.append(encodePieceChar(capturedPieces.get(i)));
            }
        }
        sb.append('|');

        // 8. Last move
        if (lastMove == null) {
            sb.append('-');
        } else {
            sb.append(lastMove.from().toAlgebraic())
                    .append(lastMove.to().toAlgebraic())
                    .append(':')
                    .append(lastMove.promotion() != null ? lastMove.promotion().name() : "-")
                    .append(':')
                    .append(lastMove.isCapture() ? '1' : '0')
                    .append(':')
                    .append(lastMove.isEnPassant() ? '1' : '0')
                    .append(':')
                    .append(lastMove.isCastle() ? '1' : '0');
        }

        return sb.toString();
    }

    /**
     * Restores a {@link GameState} from a string produced by {@link #toCompactString()}.
     *
     * @throws IllegalArgumentException if the encoded string is malformed
     */
    public static GameState fromCompactString(String encoded) {
        if (encoded == null || !encoded.startsWith("v1|")) {
            throw new IllegalArgumentException("Invalid encoded GameState");
        }
        String[] parts = encoded.split("\\|", -1);
        if (parts.length != 10) {
            throw new IllegalArgumentException("Invalid encoded GameState part count: " + parts.length);
        }

        String boardPart = parts[1];
        if (boardPart.length() != Square.BOARD_SIZE * Square.BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid board length in encoded GameState");
        }
        Piece[][] grid = new Piece[Square.BOARD_SIZE][Square.BOARD_SIZE];
        int idx = 0;
        for (int r = 0; r < Square.BOARD_SIZE; r++) {
            for (int f = 0; f < Square.BOARD_SIZE; f++) {
                grid[r][f] = decodePieceChar(boardPart.charAt(idx++));
            }
        }
        Board restoredBoard = Board.fromGridUnchecked(grid);

        Color restoredSideToMove = Color.valueOf(parts[2]);

        String castlePart = parts[3];
        if (castlePart.length() != 4) {
            throw new IllegalArgumentException("Invalid castling part in encoded GameState");
        }
        CastlingRights restoredCastling = new CastlingRights(
                castlePart.charAt(0) == 'K',
                castlePart.charAt(1) == 'Q',
                castlePart.charAt(2) == 'k',
                castlePart.charAt(3) == 'q'
        );

        Square restoredEp = "-".equals(parts[4]) ? null : Square.fromAlgebraic(parts[4]);

        String[] clockFields = parts[5].split(",", -1);
        if (clockFields.length != 4) {
            throw new IllegalArgumentException("Invalid clock part in encoded GameState");
        }
        ChessClock restoredClock = new ChessClock(
                Long.parseLong(clockFields[0]),
                Long.parseLong(clockFields[1]),
                Long.parseLong(clockFields[2]),
                Color.valueOf(clockFields[3])
        );

        GameStatus restoredStatus = GameStatus.valueOf(parts[6]);
        GameResult restoredResult = GameResult.valueOf(parts[7]);

        List<Piece> restoredCaptured = new ArrayList<>();
        if (!"-".equals(parts[8])) {
            for (int i = 0; i < parts[8].length(); i++) {
                Piece cp = decodePieceChar(parts[8].charAt(i));
                if (cp != null) {
                    restoredCaptured.add(cp);
                }
            }
        }

        Move restoredLastMove = null;
        if (!"-".equals(parts[9])) {
            String[] moveFields = parts[9].split(":", -1);
            if (moveFields.length != 5 || moveFields[0].length() != 4) {
                throw new IllegalArgumentException("Invalid lastMove part in encoded GameState");
            }
            Square from = Square.fromAlgebraic(moveFields[0].substring(0, 2));
            Square to = Square.fromAlgebraic(moveFields[0].substring(2, 4));
            PieceType promo = "-".equals(moveFields[1]) ? null : PieceType.valueOf(moveFields[1]);
            boolean isCap = "1".equals(moveFields[2]);
            boolean isEp = "1".equals(moveFields[3]);
            boolean isCastle = "1".equals(moveFields[4]);
            restoredLastMove = new Move(from, to, promo, isCap, isCastle, isEp);
        }

        return new GameState(
                restoredBoard,
                restoredSideToMove,
                restoredCastling,
                restoredEp,
                restoredClock,
                restoredStatus,
                restoredResult,
                restoredCaptured,
                restoredLastMove
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GameState that)) return false;
        return board.equals(that.board)
                && sideToMove == that.sideToMove
                && castlingRights.equals(that.castlingRights)
                && Objects.equals(enPassantTarget, that.enPassantTarget)
                && clock.equals(that.clock)
                && status == that.status
                && result == that.result
                && capturedPieces.equals(that.capturedPieces)
                && Objects.equals(lastMove, that.lastMove);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                board,
                sideToMove,
                castlingRights,
                enPassantTarget,
                clock,
                status,
                result,
                capturedPieces,
                lastMove
        );
    }

    private static char encodePieceChar(Piece piece) {
        if (piece == null) {
            return '.';
        }
        char c = switch (piece.type()) {
            case PAWN -> 'P';
            case KNIGHT -> 'N';
            case BISHOP -> 'B';
            case ROOK -> 'R';
            case QUEEN -> 'Q';
            case KING -> 'K';
        };
        return piece.color() == Color.WHITE ? c : Character.toLowerCase(c);
    }

    private static Piece decodePieceChar(char c) {
        if (c == '.') {
            return null;
        }
        Color color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
        PieceType type = switch (Character.toUpperCase(c)) {
            case 'P' -> PieceType.PAWN;
            case 'N' -> PieceType.KNIGHT;
            case 'B' -> PieceType.BISHOP;
            case 'R' -> PieceType.ROOK;
            case 'Q' -> PieceType.QUEEN;
            case 'K' -> PieceType.KING;
            default -> throw new IllegalArgumentException("Unknown piece char: " + c);
        };
        return Piece.of(color, type);
    }
}
