package com.reduan.vibechess.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable 8x8 chess board representation.
 * Provides standard chess initial setup, empty board factory for tests, piece lookup,
 * immutable updates, and point calculation.
 */
public final class Board {

    private final Piece[][] grid;

    private Board(Piece[][] grid) {
        this.grid = grid;
    }

    /**
     * Creates an empty 8x8 board with no pieces.
     */
    public static Board empty() {
        return new Board(new Piece[Square.BOARD_SIZE][Square.BOARD_SIZE]);
    }

    /**
     * Creates a board with the standard chess starting position.
     */
    public static Board initial() {
        Piece[][] g = new Piece[Square.BOARD_SIZE][Square.BOARD_SIZE];

        PieceType[] backRank = {
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
        };

        for (int file = 0; file < Square.BOARD_SIZE; file++) {
            g[0][file] = Piece.of(Color.WHITE, backRank[file]);
            g[1][file] = Piece.of(Color.WHITE, PieceType.PAWN);
            g[6][file] = Piece.of(Color.BLACK, PieceType.PAWN);
            g[7][file] = Piece.of(Color.BLACK, backRank[file]);
        }

        return new Board(g);
    }

    /**
     * Returns the piece at the given square, or {@code null} if empty.
     */
    public Piece getPieceAt(Square square) {
        Objects.requireNonNull(square, "square must not be null");
        return grid[square.rank()][square.file()];
    }

    /**
     * Returns the piece at the given 0-based rank and file, or {@code null} if empty.
     */
    public Piece getPieceAt(int rank, int file) {
        if (!Square.isValid(rank, file)) {
            throw new IllegalArgumentException(
                    "Invalid board coordinates: rank=" + rank + ", file=" + file);
        }
        return grid[rank][file];
    }

    /**
     * Returns a new {@link Board} with the specified piece placed at {@code square}
     * (pass {@code null} to clear the square).
     */
    public Board withPieceAt(Square square, Piece piece) {
        Objects.requireNonNull(square, "square must not be null");
        Piece[][] copy = copyGrid();
        copy[square.rank()][square.file()] = piece;
        return new Board(copy);
    }

    /**
     * Package-private helper for multi-square atomic updates (moves, castling, en passant).
     */
    Piece[][] copyGrid() {
        Piece[][] copy = new Piece[Square.BOARD_SIZE][Square.BOARD_SIZE];
        for (int r = 0; r < Square.BOARD_SIZE; r++) {
            System.arraycopy(grid[r], 0, copy[r], 0, Square.BOARD_SIZE);
        }
        return copy;
    }

    static Board fromGridUnchecked(Piece[][] grid) {
        return new Board(grid);
    }

    /**
     * Calculates the total points of all pieces currently on the board for {@code color}.
     * Pawn=1, Knight=2, Bishop=3, Rook=4, Queen=5, King=7.
     */
    public int points(Color color) {
        Objects.requireNonNull(color, "color must not be null");
        int sum = 0;
        for (int r = 0; r < Square.BOARD_SIZE; r++) {
            for (int f = 0; f < Square.BOARD_SIZE; f++) {
                Piece p = grid[r][f];
                if (p != null && p.color() == color) {
                    sum += p.getPoints();
                }
            }
        }
        return sum;
    }

    /**
     * Returns true if a King of the specified color is currently on the board.
     */
    public boolean hasKing(Color color) {
        Objects.requireNonNull(color, "color must not be null");
        for (int r = 0; r < Square.BOARD_SIZE; r++) {
            for (int f = 0; f < Square.BOARD_SIZE; f++) {
                Piece p = grid[r][f];
                if (p != null && p.color() == color && p.type() == PieceType.KING) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Board board)) return false;
        return Arrays.deepEquals(grid, board.grid);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(grid);
    }
}
