package com.reduan.vibechess.core;

/**
 * Immutable castling availability flags for both White and Black.
 */
public final class CastlingRights {

    private static final CastlingRights ALL = new CastlingRights(true, true, true, true);
    private static final CastlingRights NONE = new CastlingRights(false, false, false, false);

    public static final Square WHITE_KING_START = Square.fromAlgebraic("e1");
    public static final Square WHITE_ROOK_KINGSIDE = Square.fromAlgebraic("h1");
    public static final Square WHITE_ROOK_QUEENSIDE = Square.fromAlgebraic("a1");

    public static final Square BLACK_KING_START = Square.fromAlgebraic("e8");
    public static final Square BLACK_ROOK_KINGSIDE = Square.fromAlgebraic("h8");
    public static final Square BLACK_ROOK_QUEENSIDE = Square.fromAlgebraic("a8");

    private final boolean whiteKingSide;
    private final boolean whiteQueenSide;
    private final boolean blackKingSide;
    private final boolean blackQueenSide;

    public CastlingRights(boolean whiteKingSide,
                          boolean whiteQueenSide,
                          boolean blackKingSide,
                          boolean blackQueenSide) {
        this.whiteKingSide = whiteKingSide;
        this.whiteQueenSide = whiteQueenSide;
        this.blackKingSide = blackKingSide;
        this.blackQueenSide = blackQueenSide;
    }

    public static CastlingRights all() {
        return ALL;
    }

    public static CastlingRights none() {
        return NONE;
    }

    public boolean isWhiteKingSide() {
        return whiteKingSide;
    }

    public boolean isWhiteQueenSide() {
        return whiteQueenSide;
    }

    public boolean isBlackKingSide() {
        return blackKingSide;
    }

    public boolean isBlackQueenSide() {
        return blackQueenSide;
    }

    public boolean canCastleKingSide(Color color) {
        return color == Color.WHITE ? whiteKingSide : blackKingSide;
    }

    public boolean canCastleQueenSide(Color color) {
        return color == Color.WHITE ? whiteQueenSide : blackQueenSide;
    }

    /**
     * Revokes both king-side and queen-side castling rights for the given color
     * (used when that color's king moves or is captured).
     */
    public CastlingRights withoutColor(Color color) {
        if (color == Color.WHITE) {
            if (!whiteKingSide && !whiteQueenSide) return this;
            return new CastlingRights(false, false, blackKingSide, blackQueenSide);
        } else {
            if (!blackKingSide && !blackQueenSide) return this;
            return new CastlingRights(whiteKingSide, whiteQueenSide, false, false);
        }
    }

    /**
     * Revokes castling right associated with a rook starting square ("a1", "h1", "a8", "h8")
     * when a rook moves from or a piece is captured on that square.
     */
    public CastlingRights withoutRookAt(Square square) {
        if (WHITE_ROOK_KINGSIDE.equals(square) && whiteKingSide) {
            return new CastlingRights(false, whiteQueenSide, blackKingSide, blackQueenSide);
        }
        if (WHITE_ROOK_QUEENSIDE.equals(square) && whiteQueenSide) {
            return new CastlingRights(whiteKingSide, false, blackKingSide, blackQueenSide);
        }
        if (BLACK_ROOK_KINGSIDE.equals(square) && blackKingSide) {
            return new CastlingRights(whiteKingSide, whiteQueenSide, false, blackQueenSide);
        }
        if (BLACK_ROOK_QUEENSIDE.equals(square) && blackQueenSide) {
            return new CastlingRights(whiteKingSide, whiteQueenSide, blackKingSide, false);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CastlingRights that)) return false;
        return whiteKingSide == that.whiteKingSide
                && whiteQueenSide == that.whiteQueenSide
                && blackKingSide == that.blackKingSide
                && blackQueenSide == that.blackQueenSide;
    }

    @Override
    public int hashCode() {
        int result = (whiteKingSide ? 1 : 0);
        result = 31 * result + (whiteQueenSide ? 1 : 0);
        result = 31 * result + (blackKingSide ? 1 : 0);
        result = 31 * result + (blackQueenSide ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (whiteKingSide) sb.append('K');
        if (whiteQueenSide) sb.append('Q');
        if (blackKingSide) sb.append('k');
        if (blackQueenSide) sb.append('q');
        return sb.length() == 0 ? "-" : sb.toString();
    }
}
