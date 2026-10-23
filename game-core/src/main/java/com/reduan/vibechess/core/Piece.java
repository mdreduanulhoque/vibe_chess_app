package com.reduan.vibechess.core;

import java.util.Objects;

/**
 * Immutable chess piece with a {@link Color} and {@link PieceType}.
 */
public final class Piece {

    private final Color color;
    private final PieceType type;

    public Piece(Color color, PieceType type) {
        this.color = Objects.requireNonNull(color, "color must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public static Piece of(Color color, PieceType type) {
        return new Piece(color, type);
    }

    public Color getColor() {
        return color;
    }

    public Color color() {
        return color;
    }

    public PieceType getType() {
        return type;
    }

    public PieceType type() {
        return type;
    }

    public int getPoints() {
        return type.getPoints();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Piece piece)) return false;
        return color == piece.color && type == piece.type;
    }

    @Override
    public int hashCode() {
        return 31 * color.hashCode() + type.hashCode();
    }

    @Override
    public String toString() {
        return color + "_" + type;
    }
}
