package com.reduan.vibechess.core;

/**
 * Immutable board coordinate representing a square on the 8x8 chess board.
 * <ul>
 *   <li>{@code rank} (or {@code row}): 0..7, where 0 is rank '1' (White's back rank) and 7 is rank '8' (Black's back rank).</li>
 *   <li>{@code file} (or {@code col}): 0..7, where 0 is file 'a' and 7 is file 'h'.</li>
 * </ul>
 */
public final class Square {

    public static final int BOARD_SIZE = 8;

    private static final Square[][] CACHE = new Square[BOARD_SIZE][BOARD_SIZE];

    static {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int f = 0; f < BOARD_SIZE; f++) {
                CACHE[r][f] = new Square(r, f);
            }
        }
    }

    private final int rank;
    private final int file;

    private Square(int rank, int file) {
        this.rank = rank;
        this.file = file;
    }

    /**
     * Returns the Square for the given 0-based rank (0..7) and file (0..7).
     *
     * @throws IllegalArgumentException if rank or file is out of bounds.
     */
    public static Square of(int rank, int file) {
        if (!isValid(rank, file)) {
            throw new IllegalArgumentException(
                    "Invalid square coordinates: rank=" + rank + ", file=" + file);
        }
        return CACHE[rank][file];
    }

    /**
     * Parses standard algebraic notation such as "a1", "e4", "h8".
     */
    public static Square fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic square: " + algebraic);
        }
        int file = algebraic.charAt(0) - 'a';
        int rank = algebraic.charAt(1) - '1';
        return of(rank, file);
    }

    /**
     * Checks whether the 0-based rank and file are on the 8x8 board.
     */
    public static boolean isValid(int rank, int file) {
        return rank >= 0 && rank < BOARD_SIZE && file >= 0 && file < BOARD_SIZE;
    }

    public int getRank() {
        return rank;
    }

    public int rank() {
        return rank;
    }

    public int getRow() {
        return rank;
    }

    public int row() {
        return rank;
    }

    public int getFile() {
        return file;
    }

    public int file() {
        return file;
    }

    public int getCol() {
        return file;
    }

    public int col() {
        return file;
    }

    /**
     * Returns algebraic string representation (e.g., "e4").
     */
    public String toAlgebraic() {
        return "" + (char) ('a' + file) + (char) ('1' + rank);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Square square)) return false;
        return rank == square.rank && file == square.file;
    }

    @Override
    public int hashCode() {
        return (rank << 3) | file;
    }

    @Override
    public String toString() {
        return toAlgebraic();
    }
}
