package com.reduan.vibechess.ui.game;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.reduan.vibechess.R;

/**
 * Horizontal split bar visualizing White vs Black winning probability percentage,
 * smoothly animating transitions whenever pieces are captured or clocks change.
 */
public class WinProbabilityBarView extends View {

    private static final long ANIMATION_DURATION_MS = 220L;

    private final Paint whiteBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blackBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final Path clipPath = new Path();

    private float displayedWhiteFraction = 0.5f;
    private float targetWhiteFraction = 0.5f;
    @Nullable
    private ValueAnimator animator;

    public WinProbabilityBarView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public WinProbabilityBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WinProbabilityBarView(@NonNull Context context,
                                 @Nullable AttributeSet attrs,
                                 int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        whiteBarPaint.setStyle(Paint.Style.FILL);
        whiteBarPaint.setColor(ContextCompat.getColor(context, R.color.primary));

        blackBarPaint.setStyle(Paint.Style.FILL);
        blackBarPaint.setColor(ContextCompat.getColor(context, R.color.piece_black_fill));

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(
                context.getResources().getDimension(R.dimen.stroke_width)
        );
        borderPaint.setColor(ContextCompat.getColor(context, R.color.stroke_subtle));
    }

    /**
     * Updates the target White winning percentage (0..100) and smoothly animates the split bar.
     */
    public void setWhitePercent(int whitePercent, boolean animate) {
        float clamped = Math.max(0f, Math.min(1f, whitePercent / 100f));
        if (Math.abs(targetWhiteFraction - clamped) < 0.001f) {
            return;
        }
        targetWhiteFraction = clamped;

        if (animator != null) {
            animator.cancel();
            animator = null;
        }

        if (!animate) {
            displayedWhiteFraction = clamped;
            invalidate();
            return;
        }

        ValueAnimator va = ValueAnimator.ofFloat(displayedWhiteFraction, clamped);
        va.setDuration(ANIMATION_DURATION_MS);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(anim -> {
            displayedWhiteFraction = (float) anim.getAnimatedValue();
            invalidate();
        });
        this.animator = va;
        va.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        float radius = h / 2f;
        barRect.set(0f, 0f, w, h);

        clipPath.reset();
        clipPath.addRoundRect(barRect, radius, radius, Path.Direction.CW);

        int save = canvas.save();
        canvas.clipPath(clipPath);

        float splitX = w * displayedWhiteFraction;
        if (splitX > 0f) {
            canvas.drawRect(0f, 0f, splitX, h, whiteBarPaint);
        }
        if (splitX < w) {
            canvas.drawRect(splitX, 0f, w, h, blackBarPaint);
        }
        canvas.restoreToCount(save);

        float halfStroke = borderPaint.getStrokeWidth() / 2f;
        barRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);
        canvas.drawRoundRect(barRect, radius, radius, borderPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }
}
