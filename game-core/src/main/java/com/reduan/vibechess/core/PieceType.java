package com.reduan.vibechess.core;

import java.util.List;

/**
 * Chess piece types and their Vibe Chess point values per PROJECT_SPEC.md:
 * Pawn = 1, Knight = 2, Bishop = 3, Rook = 4, Queen = 5, King = 7.
 */
public enum PieceType {
    PAWN(1),
    KNIGHT(2),
    BISHOP(3),
    ROOK(4),
    QUEEN(5),
    KING(7);

    /**
     * The 4 piece types a pawn may promote to, ordered from highest to lowest value.
     */
    public static final List<PieceType> PROMOTION_CHOICES = List.of(
            QUEEN, ROOK, BISHOP, KNIGHT
    );

    private final int points;

    PieceType(int points) {
        this.points = points;
    }

    /**
     * Returns the point value of this piece type on the board.
     */
    public int getPoints() {
        return points;
    }

    /**
     * Alias for {@link #getPoints()}.
     */
    public int points() {
        return points;
    }

    /**
     * Returns true if a pawn can promote into this piece type.
     */
    public boolean isValidPromotion() {
        return this == QUEEN || this == ROOK || this == BISHOP || this == KNIGHT;
    }
}
