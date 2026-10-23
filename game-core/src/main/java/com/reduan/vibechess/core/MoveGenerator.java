package com.reduan.vibechess.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generates all Vibe Chess moves for the side to move.
 * <p>
 * Rules per PROJECT_SPEC.md:
 * <ul>
 *   <li>NO check, NO checkmate, NO stalemate.</li>
 *   <li>A king may move into an attacked square or be left on an attacked square.</li>
 *   <li>A king may be captured like any other piece.</li>
 *   <li>Castling requires only that the king and rook have not moved and squares between are empty
 *       (attacked squares do NOT block castling).</li>
 *   <li>En passant is allowed only immediately after the opponent's double pawn step.</li>
 *   <li>Pawn promotion produces 4 choices: Queen, Rook, Bishop, Knight.</li>
 * </ul>
 */
public final class MoveGenerator {

    private static final int[][] KNIGHT_OFFSETS = {
            {-2, -1}, {-2, 1},
            {-1, -2}, {-1, 2},
            {1, -2},  {1, 2},
            {2, -1},  {2, 1}
    };

    private static final int[][] BISHOP_DIRS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private static final int[][] ROOK_DIRS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    private static final int[][] QUEEN_KING_DIRS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    private MoveGenerator() {
    }

    /**
     * Generates all valid Vibe Chess moves for {@code state.getSideToMove()}.
     * Returns an empty list if the game is already over.
     */
    public static List<Move> generateMoves(GameState state) {
        Objects.requireNonNull(state, "state must not be null");
        if (state.isGameOver()) {
            return Collections.emptyList();
        }

        List<Move> moves = new ArrayList<>(48);
        Board board = state.getBoard();
        Color side = state.getSideToMove();

        for (int rank = 0; rank < Square.BOARD_SIZE; rank++) {
            for (int file = 0; file < Square.BOARD_SIZE; file++) {
                Piece piece = board.getPieceAt(rank, file);
                if (piece == null || piece.color() != side) {
                    continue;
                }
                Square from = Square.of(rank, file);
                generateMovesForSquare(state, from, piece, moves);
            }
        }

        return moves;
    }

    /**
     * Generates all valid moves originating from a specific square in the given state.
     */
    public static List<Move> generateMovesFrom(GameState state, Square from) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(from, "from must not be null");
        if (state.isGameOver()) {
            return Collections.emptyList();
        }
        Piece piece = state.getBoard().getPieceAt(from);
        if (piece == null || piece.color() != state.getSideToMove()) {
            return Collections.emptyList();
        }
        List<Move> moves = new ArrayList<>(16);
        generateMovesForSquare(state, from, piece, moves);
        return moves;
    }

    private static void generateMovesForSquare(GameState state,
                                               Square from,
                                               Piece piece,
                                               List<Move> out) {
        Board board = state.getBoard();
        Color side = piece.color();
        switch (piece.type()) {
            case PAWN -> generatePawnMoves(state, from, side, out);
            case KNIGHT -> generateJumpMoves(board, from, side, KNIGHT_OFFSETS, out);
            case BISHOP -> generateSlidingMoves(board, from, side, BISHOP_DIRS, out);
            case ROOK -> generateSlidingMoves(board, from, side, ROOK_DIRS, out);
            case QUEEN -> generateSlidingMoves(board, from, side, QUEEN_KING_DIRS, out);
            case KING -> {
                generateJumpMoves(board, from, side, QUEEN_KING_DIRS, out);
                generateCastlingMoves(state, from, side, out);
            }
        }
    }

    private static void generatePawnMoves(GameState state,
                                          Square from,
                                          Color side,
                                          List<Move> out) {
        Board board = state.getBoard();
        int dir = (side == Color.WHITE) ? 1 : -1;
        int startRank = (side == Color.WHITE) ? 1 : 6;
        int promotionRank = (side == Color.WHITE) ? 7 : 0;

        int nextRank = from.rank() + dir;
        int file = from.file();

        // 1. Single step forward
        if (Square.isValid(nextRank, file) && board.getPieceAt(nextRank, file) == null) {
            Square to = Square.of(nextRank, file);
            if (nextRank == promotionRank) {
                addPromotions(from, to, false, out);
            } else {
                out.add(Move.quiet(from, to));

                // 2. Double step forward from starting rank
                int doubleRank = from.rank() + 2 * dir;
                if (from.rank() == startRank && board.getPieceAt(doubleRank, file) == null) {
                    out.add(Move.quiet(from, Square.of(doubleRank, file)));
                }
            }
        }

        // 3. Diagonal captures & en passant
        Square epTarget = state.getEnPassantTarget();
        for (int df : new int[]{-1, 1}) {
            int targetFile = file + df;
            if (!Square.isValid(nextRank, targetFile)) {
                continue;
            }
            Square to = Square.of(nextRank, targetFile);
            Piece targetPiece = board.getPieceAt(to);

            if (targetPiece != null && targetPiece.color() != side) {
                if (nextRank == promotionRank) {
                    addPromotions(from, to, true, out);
                } else {
                    out.add(Move.capture(from, to));
                }
            } else if (targetPiece == null && to.equals(epTarget)) {
                // Verify the opponent's double-stepped pawn is adjacent on (from.rank, targetFile)
                Piece epPawn = board.getPieceAt(from.rank(), targetFile);
                if (epPawn != null
                        && epPawn.color() == side.opposite()
                        && epPawn.type() == PieceType.PAWN) {
                    out.add(Move.enPassant(from, to));
                }
            }
        }
    }

    private static void addPromotions(Square from, Square to, boolean capture, List<Move> out) {
        for (PieceType promoType : PieceType.PROMOTION_CHOICES) {
            out.add(Move.promotion(from, to, promoType, capture));
        }
    }

    private static void generateJumpMoves(Board board,
                                          Square from,
                                          Color side,
                                          int[][] offsets,
                                          List<Move> out) {
        for (int[] offset : offsets) {
            int r = from.rank() + offset[0];
            int f = from.file() + offset[1];
            if (!Square.isValid(r, f)) {
                continue;
            }
            Piece target = board.getPieceAt(r, f);
            Square to = Square.of(r, f);
            if (target == null) {
                out.add(Move.quiet(from, to));
            } else if (target.color() != side) {
                out.add(Move.capture(from, to));
            }
        }
    }

    private static void generateSlidingMoves(Board board,
                                             Square from,
                                             Color side,
                                             int[][] dirs,
                                             List<Move> out) {
        for (int[] dir : dirs) {
            int r = from.rank() + dir[0];
            int f = from.file() + dir[1];
            while (Square.isValid(r, f)) {
                Piece target = board.getPieceAt(r, f);
                Square to = Square.of(r, f);
                if (target == null) {
                    out.add(Move.quiet(from, to));
                } else {
                    if (target.color() != side) {
                        out.add(Move.capture(from, to));
                    }
                    break;
                }
                r += dir[0];
                f += dir[1];
            }
        }
    }

    private static void generateCastlingMoves(GameState state,
                                              Square from,
                                              Color side,
                                              List<Move> out) {
        CastlingRights rights = state.getCastlingRights();
        Board board = state.getBoard();
        int backRank = (side == Color.WHITE) ? 0 : 7;

        // King must be on its starting square (e1 for White, e8 for Black)
        if (from.rank() != backRank || from.file() != 4) {
            return;
        }

        // King-side castling (e1->g1 or e8->g8): f and g files (5, 6) must be empty, rook at h (7)
        if (rights.canCastleKingSide(side)) {
            Piece rook = board.getPieceAt(backRank, 7);
            if (rook != null && rook.color() == side && rook.type() == PieceType.ROOK
                    && board.getPieceAt(backRank, 5) == null
                    && board.getPieceAt(backRank, 6) == null) {
                out.add(Move.castle(from, Square.of(backRank, 6)));
            }
        }

        // Queen-side castling (e1->c1 or e8->c8): b, c, d files (1, 2, 3) must be empty, rook at a (0)
        if (rights.canCastleQueenSide(side)) {
            Piece rook = board.getPieceAt(backRank, 0);
            if (rook != null && rook.color() == side && rook.type() == PieceType.ROOK
                    && board.getPieceAt(backRank, 1) == null
                    && board.getPieceAt(backRank, 2) == null
                    && board.getPieceAt(backRank, 3) == null) {
                out.add(Move.castle(from, Square.of(backRank, 2)));
            }
        }
    }
}
