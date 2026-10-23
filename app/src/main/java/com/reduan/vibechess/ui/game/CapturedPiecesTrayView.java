package com.reduan.vibechess.ui.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.reduan.vibechess.core.Piece;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Renders a row of captured piece icons (icons only, no numbers).
 * Automatically adjusts horizontal spacing when many pieces are captured so all icons fit cleanly.
 */
public class CapturedPiecesTrayView extends View {

    private static final int DEFAULT_ICON_SIZE_DP = 22;

    @NonNull
    private List<Piece> capturedPieces = List.of();
    private boolean rotateIcons180 = false;

    public CapturedPiecesTrayView(@NonNull Context context) {
        super(context);
    }

    public CapturedPiecesTrayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CapturedPiecesTrayView(@NonNull Context context,
                                  @Nullable AttributeSet attrs,
                                  int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Updates the list of captured pieces displayed in this tray, sorted by highest piece value first.
     */
    public void setCapturedPieces(@Nullable List<Piece> pieces) {
        List<Piece> sorted;
        if (pieces == null || pieces.isEmpty()) {
            sorted = List.of();
        } else {
            sorted = new ArrayList<>(pieces);
            sorted.sort(Comparator.comparingInt(Piece::getPoints).reversed());
        }
        if (!Objects.equals(this.capturedPieces, sorted)) {
            this.capturedPieces = sorted;
            invalidate();
        }
    }

    public void setRotateIcons180(boolean rotate) {
        if (this.rotateIcons180 != rotate) {
            this.rotateIcons180 = rotate;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultHeight = dpToPx(DEFAULT_ICON_SIZE_DP);
        int width = resolveSize(dpToPx(120), widthMeasureSpec);
        int height = resolveSize(defaultHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (capturedPieces.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int iconSize = Math.min(height, dpToPx(DEFAULT_ICON_SIZE_DP));
        int count = capturedPieces.size();

        float defaultStep = iconSize * 0.82f;
        float step;
        if (count <= 1 || iconSize + (count - 1) * defaultStep <= width) {
            step = defaultStep;
        } else {
            step = Math.max(4f, (width - iconSize) / (float) (count - 1));
        }

        int top = (height - iconSize) / 2;
        int bottom = top + iconSize;

        Context context = getContext();
        for (int i = 0; i < count; i++) {
            Piece piece = capturedPieces.get(i);
            Drawable drawable = AppCompatResources.getDrawable(
                    context,
                    PieceDrawableMapper.getDrawableRes(piece)
            );
            if (drawable == null) {
                continue;
            }
            int left = Math.round(i * step);
            int right = left + iconSize;
            drawable.setBounds(left, top, right, bottom);

            if (rotateIcons180) {
                canvas.save();
                canvas.rotate(180f, left + iconSize / 2f, top + iconSize / 2f);
                drawable.draw(canvas);
                canvas.restore();
            } else {
                drawable.draw(canvas);
            }
        }
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }
}
