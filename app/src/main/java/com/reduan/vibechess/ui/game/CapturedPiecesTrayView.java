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

import com.reduan.vibechess.R;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.Piece;
import com.reduan.vibechess.core.PieceType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders a row of captured piece icons (icons only, no numbers).
 * Automatically adjusts horizontal spacing when many pieces are captured so all icons fit cleanly.
 * Preloads piece drawables once at initialization so {@link #onDraw(Canvas)} performs zero allocations.
 */
public class CapturedPiecesTrayView extends View {

    private static final int DEFAULT_ICON_SIZE_DP = 22;

    private final Map<Color, Map<PieceType, Drawable>> drawableCache = new EnumMap<>(Color.class);
    private int defaultIconSizePx;

    @NonNull
    private List<Piece> capturedPieces = List.of();
    private boolean rotateIcons180 = false;

    public CapturedPiecesTrayView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CapturedPiecesTrayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CapturedPiecesTrayView(@NonNull Context context,
                                  @Nullable AttributeSet attrs,
                                  int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        this.defaultIconSizePx = dpToPx(DEFAULT_ICON_SIZE_DP);
        for (Color color : Color.values()) {
            Map<PieceType, Drawable> byType = new EnumMap<>(PieceType.class);
            for (PieceType type : PieceType.values()) {
                Drawable d = AppCompatResources.getDrawable(
                        context,
                        PieceDrawableMapper.getDrawableRes(color, type)
                );
                if (d != null) {
                    byType.put(type, d.mutate());
                }
            }
            drawableCache.put(color, byType);
        }
        updateAccessibilityDescription();
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
            updateAccessibilityDescription();
            invalidate();
        }
    }

    private void updateAccessibilityDescription() {
        Context context = getContext();
        if (capturedPieces.isEmpty()) {
            setContentDescription(context.getString(R.string.game_captured_tray_placeholder));
            return;
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0, n = capturedPieces.size(); i < n; i++) {
            if (i > 0) {
                names.append(", ");
            }
            Piece piece = capturedPieces.get(i);
            names.append(context.getString(PieceDrawableMapper.getPieceTypeLabelRes(piece.type())));
        }
        setContentDescription(context.getString(R.string.a11y_captured_tray_count, names.toString()));
    }

    public void setRotateIcons180(boolean rotate) {
        if (this.rotateIcons180 != rotate) {
            this.rotateIcons180 = rotate;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(dpToPx(120), widthMeasureSpec);
        int height = resolveSize(defaultIconSizePx, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int count = capturedPieces.size();
        if (count == 0) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int iconSize = Math.min(height, defaultIconSizePx);
        float defaultStep = iconSize * 0.82f;
        float step;
        if (count <= 1 || iconSize + (count - 1) * defaultStep <= width) {
            step = defaultStep;
        } else {
            step = Math.max(4f, (width - iconSize) / (float) (count - 1));
        }

        int top = (height - iconSize) / 2;
        int bottom = top + iconSize;

        for (int i = 0; i < count; i++) {
            Piece piece = capturedPieces.get(i);
            Map<PieceType, Drawable> byType = drawableCache.get(piece.color());
            Drawable drawable = byType != null ? byType.get(piece.type()) : null;
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
