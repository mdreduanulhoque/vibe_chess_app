package com.reduan.vibechess.core;

import java.util.Objects;

/**
 * Immutable chess move with origin square, destination square, optional promotion piece type,
 * and flags for capture, castling, and en passant.
 */
public final class Move {

    private final Square from;
    private final Square to;
    private final PieceType promotion;
    private final boolean capture;
    private final boolean castle;
    private final boolean enPassant;

    public Move(Square from,
                Square to,
                PieceType promotion,
                boolean capture,
                boolean castle,
                boolean enPassant) {
        this.from = Objects.requireNonNull(from, "from must not be null");
        this.to = Objects.requireNonNull(to, "to must not be null");
        if (promotion != null && !promotion.isValidPromotion()) {
            throw new IllegalArgumentException("Invalid promotion piece type: " + promotion);
        }
        this.promotion = promotion;
        this.capture = capture;
        this.castle = castle;
        this.enPassant = enPassant;
    }

    public static Move quiet(Square from, Square to) {
        return new Move(from, to, null, false, false, false);
    }

    public static Move capture(Square from, Square to) {
        return new Move(from, to, null, true, false, false);
    }

    public static Move promotion(Square from, Square to, PieceType promotion, boolean capture) {
        return new Move(from, to, promotion, capture, false, false);
    }

    public static Move castle(Square from, Square to) {
        return new Move(from, to, null, false, true, false);
    }

    public static Move enPassant(Square from, Square to) {
        return new Move(from, to, null, true, false, true);
    }

    public Square getFrom() {
        return from;
    }

    public Square from() {
        return from;
    }

    public Square getTo() {
        return to;
    }

    public Square to() {
        return to;
    }

    public PieceType getPromotion() {
        return promotion;
    }

    public PieceType promotion() {
        return promotion;
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    public boolean isCapture() {
        return capture;
    }

    public boolean capture() {
        return capture;
    }

    public boolean isCastle() {
        return castle;
    }

    public boolean castle() {
        return castle;
    }

    public boolean isEnPassant() {
        return enPassant;
    }

    public boolean enPassant() {
        return enPassant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move move)) return false;
        return capture == move.capture
                && castle == move.castle
                && enPassant == move.enPassant
                && from.equals(move.from)
                && to.equals(move.to)
                && promotion == move.promotion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, promotion, capture, castle, enPassant);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(from.toAlgebraic()).append(capture ? "x" : "-").append(to.toAlgebraic());
        if (promotion != null) {
            sb.append("=").append(promotion.name().charAt(0));
        }
        if (castle) {
            sb.append(" (castle)");
        }
        if (enPassant) {
            sb.append(" (e.p.)");
        }
        return sb.toString();
    }
}
