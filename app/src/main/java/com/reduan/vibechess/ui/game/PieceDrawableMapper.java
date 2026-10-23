package com.reduan.vibechess.ui.game;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.reduan.vibechess.R;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.Piece;
import com.reduan.vibechess.core.PieceType;

/**
 * Maps {@link Piece} / ({@link Color}, {@link PieceType}) to the corresponding minimalist
 * VectorDrawable resource and localized label string resource.
 */
public final class PieceDrawableMapper {

    private PieceDrawableMapper() {
    }

    @DrawableRes
    public static int getDrawableRes(@NonNull Piece piece) {
        return getDrawableRes(piece.color(), piece.type());
    }

    @DrawableRes
    public static int getDrawableRes(@NonNull Color color, @NonNull PieceType type) {
        if (color == Color.WHITE) {
            return switch (type) {
                case PAWN -> R.drawable.ic_piece_white_pawn;
                case KNIGHT -> R.drawable.ic_piece_white_knight;
                case BISHOP -> R.drawable.ic_piece_white_bishop;
                case ROOK -> R.drawable.ic_piece_white_rook;
                case QUEEN -> R.drawable.ic_piece_white_queen;
                case KING -> R.drawable.ic_piece_white_king;
            };
        } else {
            return switch (type) {
                case PAWN -> R.drawable.ic_piece_black_pawn;
                case KNIGHT -> R.drawable.ic_piece_black_knight;
                case BISHOP -> R.drawable.ic_piece_black_bishop;
                case ROOK -> R.drawable.ic_piece_black_rook;
                case QUEEN -> R.drawable.ic_piece_black_queen;
                case KING -> R.drawable.ic_piece_black_king;
            };
        }
    }

    @StringRes
    public static int getPieceTypeLabelRes(@NonNull PieceType type) {
        return switch (type) {
            case PAWN -> R.string.piece_pawn;
            case KNIGHT -> R.string.piece_knight;
            case BISHOP -> R.string.piece_bishop;
            case ROOK -> R.string.piece_rook;
            case QUEEN -> R.string.piece_queen;
            case KING -> R.string.piece_king;
        };
    }

    @StringRes
    public static int getColorLabelRes(@NonNull Color color) {
        return color == Color.WHITE ? R.string.game_player_white : R.string.game_player_black;
    }

    @StringRes
    public static int getPromotionPieceLabelRes(@NonNull PieceType type) {
        return switch (type) {
            case QUEEN -> R.string.piece_queen;
            case ROOK -> R.string.piece_rook;
            case BISHOP -> R.string.piece_bishop;
            case KNIGHT -> R.string.piece_knight;
            default -> throw new IllegalArgumentException("Not a valid promotion piece: " + type);
        };
    }
}
