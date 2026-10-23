package com.reduan.vibechess.ui.game;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.Move;
import com.reduan.vibechess.core.PieceType;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Displays the minimalist Pawn Promotion picker dialog (Queen, Rook, Bishop, Knight)
 * and resolves the corresponding {@link Move} from the core's legal promotion options.
 */
public final class PromotionDialogHelper {

    public interface PromotionSelectionCallback {
        void onPromotionMoveSelected(@NonNull Move chosenMove);

        default void onPromotionCancelled() {
        }
    }

    private PromotionDialogHelper() {
    }

    @NonNull
    public static AlertDialog show(@NonNull Context context,
                                   @NonNull Color sideToMove,
                                   @NonNull List<Move> promotionMoves,
                                   @NonNull PromotionSelectionCallback callback) {
        View contentView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_promotion_picker, null, false);

        ImageView ivQueen = contentView.findViewById(R.id.iv_promote_queen);
        ImageView ivRook = contentView.findViewById(R.id.iv_promote_rook);
        ImageView ivBishop = contentView.findViewById(R.id.iv_promote_bishop);
        ImageView ivKnight = contentView.findViewById(R.id.iv_promote_knight);

        ivQueen.setImageResource(PieceDrawableMapper.getDrawableRes(sideToMove, PieceType.QUEEN));
        ivRook.setImageResource(PieceDrawableMapper.getDrawableRes(sideToMove, PieceType.ROOK));
        ivBishop.setImageResource(PieceDrawableMapper.getDrawableRes(sideToMove, PieceType.BISHOP));
        ivKnight.setImageResource(PieceDrawableMapper.getDrawableRes(sideToMove, PieceType.KNIGHT));

        AtomicBoolean selectionHandled = new AtomicBoolean(false);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(contentView)
                .setCancelable(true)
                .setOnCancelListener(d -> {
                    if (selectionHandled.compareAndSet(false, true)) {
                        callback.onPromotionCancelled();
                    }
                })
                .create();

        bindChoice(contentView.findViewById(R.id.card_promote_queen),
                PieceType.QUEEN, promotionMoves, dialog, selectionHandled, callback);
        bindChoice(contentView.findViewById(R.id.card_promote_rook),
                PieceType.ROOK, promotionMoves, dialog, selectionHandled, callback);
        bindChoice(contentView.findViewById(R.id.card_promote_bishop),
                PieceType.BISHOP, promotionMoves, dialog, selectionHandled, callback);
        bindChoice(contentView.findViewById(R.id.card_promote_knight),
                PieceType.KNIGHT, promotionMoves, dialog, selectionHandled, callback);

        dialog.show();
        return dialog;
    }

    private static void bindChoice(@NonNull View cardView,
                                   @NonNull PieceType pieceType,
                                   @NonNull List<Move> promotionMoves,
                                   @NonNull AlertDialog dialog,
                                   @NonNull AtomicBoolean selectionHandled,
                                   @NonNull PromotionSelectionCallback callback) {
        cardView.setContentDescription(
                cardView.getContext().getString(PieceDrawableMapper.getPromotionPieceLabelRes(pieceType))
        );
        cardView.setOnClickListener(v -> {
            if (!selectionHandled.compareAndSet(false, true)) {
                return;
            }
            Move chosen = findMoveForPromotion(promotionMoves, pieceType);
            dialog.dismiss();
            if (chosen != null) {
                callback.onPromotionMoveSelected(chosen);
            }
        });
    }

    @Nullable
    public static Move findMoveForPromotion(@NonNull List<Move> promotionMoves,
                                            @NonNull PieceType promotionType) {
        for (Move move : promotionMoves) {
            if (move.promotion() == promotionType) {
                return move;
            }
        }
        return promotionMoves.isEmpty() ? null : promotionMoves.get(0);
    }
}
