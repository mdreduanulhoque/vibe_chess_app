package com.reduan.vibechess;

import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.Move;
import com.reduan.vibechess.core.Piece;
import com.reduan.vibechess.core.PieceType;
import com.reduan.vibechess.core.Square;
import com.reduan.vibechess.ui.game.PieceDrawableMapper;
import com.reduan.vibechess.ui.game.PromotionDialogHelper;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests verifying the 12 piece VectorDrawable mappings and promotion choice resolution.
 */
public class PieceDrawableMapperTest {

    @Test
    public void allTwelvePieceCombinationsMapToDistinctDrawables() {
        Set<Integer> drawableIds = new HashSet<>();
        for (Color color : Color.values()) {
            for (PieceType type : PieceType.values()) {
                int resId = PieceDrawableMapper.getDrawableRes(Piece.of(color, type));
                assertNotEquals(0, resId);
                drawableIds.add(resId);
            }
        }
        assertEquals(12, drawableIds.size());
    }

    @Test
    public void promotionChoicesResolveCorrectMove() {
        Square from = Square.fromAlgebraic("a7");
        Square to = Square.fromAlgebraic("a8");
        List<Move> promotions = List.of(
                Move.promotion(from, to, PieceType.QUEEN, false),
                Move.promotion(from, to, PieceType.ROOK, false),
                Move.promotion(from, to, PieceType.BISHOP, false),
                Move.promotion(from, to, PieceType.KNIGHT, false)
        );

        for (PieceType type : PieceType.PROMOTION_CHOICES) {
            Move resolved = PromotionDialogHelper.findMoveForPromotion(promotions, type);
            assertNotNull(resolved);
            assertEquals(type, resolved.promotion());
            assertNotEquals(0, PieceDrawableMapper.getPromotionPieceLabelRes(type));
        }
    }

    @Test
    public void accessibilityPieceAndColorLabelsMapToDistinctResources() {
        Set<Integer> typeLabelIds = new HashSet<>();
        for (PieceType type : PieceType.values()) {
            int resId = PieceDrawableMapper.getPieceTypeLabelRes(type);
            assertNotEquals(0, resId);
            typeLabelIds.add(resId);
        }
        assertEquals(6, typeLabelIds.size());

        Set<Integer> colorLabelIds = new HashSet<>();
        for (Color color : Color.values()) {
            int resId = PieceDrawableMapper.getColorLabelRes(color);
            assertNotEquals(0, resId);
            colorLabelIds.add(resId);
        }
        assertEquals(2, colorLabelIds.size());
    }
}
