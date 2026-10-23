package com.reduan.vibechess.ui.game;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.reduan.vibechess.R;
import com.reduan.vibechess.core.Board;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.GameState;
import com.reduan.vibechess.core.Move;
import com.reduan.vibechess.core.Piece;
import com.reduan.vibechess.core.PieceType;
import com.reduan.vibechess.core.Square;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Custom Canvas-drawn Chess Board View supporting both:
 * <ul>
 *   <li>Live interactive 8x8 gameplay ({@link #setGameState(GameState)}).</li>
 *   <li>Read-only 5x5 or 8x8 "demo" mode ({@link #setDemoConfig(BoardDemoConfig)}) for visual
 *       rule illustrations on the "How to Play" screen (with move dots, capture rings,
 *       directional arrows, and gentle pulsing highlights).</li>
 * </ul>
 */
public class BoardView extends View {

    private static final long MOVE_ANIMATION_DURATION_MS = 175L;
    private static final long DEMO_PULSE_DURATION_MS = 1100L;
    private static final int DEFAULT_BOARD_DP = 320;

    public interface OnMoveSelectedListener {
        void onMoveSelected(@NonNull Move move);
    }

    public interface OnSquareSelectedListener {
        void onSquareSelected(@Nullable Square square);
    }

    public interface OnPromotionRequestedListener {
        void onPromotionRequested(@NonNull Color sideToMove, @NonNull List<Move> promotionMoves);
    }

    private final Paint lightSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint darkSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedSquarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lastMovePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint moveDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint captureRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint boardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint demoPulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint demoArrowLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint demoArrowHeadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path arrowHeadPath = new Path();

    private final RectF squareRect = new RectF();
    private final Map<Color, Map<PieceType, Drawable>> drawableCache = new EnumMap<>(Color.class);

    @Nullable
    private GameState gameState;
    @Nullable
    private BoardDemoConfig demoConfig;
    @Nullable
    private Square selectedSquare;
    @NonNull
    private List<Move> allLegalMoves = List.of();
    @NonNull
    private List<Move> legalMovesFromSelected = List.of();

    private boolean rotateBlackPieces = true;
    private boolean boardPerspectiveFlipped = false;
    private boolean interactionEnabled = true;

    @Nullable
    private OnMoveSelectedListener onMoveSelectedListener;
    @Nullable
    private OnSquareSelectedListener onSquareSelectedListener;
    @Nullable
    private OnPromotionRequestedListener onPromotionRequestedListener;

    // Move animation state (175ms)
    @Nullable
    private Move animatingMove;
    private float animationProgress = 1f;
    @Nullable
    private ValueAnimator moveAnimator;
    private boolean hasRenderedInitialState = false;

    // Demo mode gentle pulsing highlight state
    private int baseHighlightAlpha = 102;
    private float demoPulseFactor = 0.85f;
    @Nullable
    private ValueAnimator demoPulseAnimator;

    public BoardView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BoardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BoardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        lightSquarePaint.setStyle(Paint.Style.FILL);
        lightSquarePaint.setColor(ContextCompat.getColor(context, R.color.board_light));

        darkSquarePaint.setStyle(Paint.Style.FILL);
        darkSquarePaint.setColor(ContextCompat.getColor(context, R.color.board_dark));

        int highlightMoveColor = ContextCompat.getColor(context, R.color.highlight_move);
        selectedSquarePaint.setStyle(Paint.Style.FILL);
        selectedSquarePaint.setColor(highlightMoveColor);

        lastMovePaint.setStyle(Paint.Style.FILL);
        lastMovePaint.setColor(ContextCompat.getColor(context, R.color.last_move));

        moveDotPaint.setStyle(Paint.Style.FILL);
        moveDotPaint.setColor(highlightMoveColor);

        captureRingPaint.setStyle(Paint.Style.STROKE);
        captureRingPaint.setColor(highlightMoveColor);

        boardBorderPaint.setStyle(Paint.Style.STROKE);
        boardBorderPaint.setStrokeWidth(dpToPx(1));
        boardBorderPaint.setColor(ContextCompat.getColor(context, R.color.stroke_subtle));

        demoPulsePaint.setStyle(Paint.Style.FILL);
        demoPulsePaint.setColor(highlightMoveColor);
        baseHighlightAlpha = android.graphics.Color.alpha(highlightMoveColor);

        int primaryColor = ContextCompat.getColor(context, R.color.primary);
        demoArrowLinePaint.setStyle(Paint.Style.STROKE);
        demoArrowLinePaint.setStrokeCap(Paint.Cap.ROUND);
        demoArrowLinePaint.setColor(primaryColor);

        demoArrowHeadPaint.setStyle(Paint.Style.FILL);
        demoArrowHeadPaint.setColor(primaryColor);

        preloadDrawables(context);
    }

    private void preloadDrawables(@NonNull Context context) {
        for (Color color : Color.values()) {
            Map<PieceType, Drawable> byType = new EnumMap<>(PieceType.class);
            for (PieceType type : PieceType.values()) {
                int resId = PieceDrawableMapper.getDrawableRes(color, type);
                Drawable drawable = AppCompatResources.getDrawable(context, resId);
                if (drawable != null) {
                    byType.put(type, drawable.mutate());
                }
            }
            drawableCache.put(color, byType);
        }
    }

    /**
     * Configures this {@link BoardView} in read-only "demo" mode for visual rule illustrations.
     */
    public void setDemoConfig(@Nullable BoardDemoConfig config) {
        this.demoConfig = config;
        if (config != null) {
            this.gameState = null;
            this.selectedSquare = null;
            this.interactionEnabled = false;
            this.rotateBlackPieces = false;
            this.boardPerspectiveFlipped = false;
            cancelMoveAnimation();
            updateDemoPulseAnimation();
        } else {
            cancelDemoPulseAnimation();
        }
        invalidate();
    }

    @Nullable
    public BoardDemoConfig getDemoConfig() {
        return demoConfig;
    }

    /**
     * Updates the board with a new {@link GameState} from {@code :game-core}.
     * Automatically triggers a light 175ms slide animation when a new move has been played.
     */
    public void setGameState(@Nullable GameState newState) {
        this.demoConfig = null;
        cancelDemoPulseAnimation();

        Move previousLastMove = this.gameState != null ? this.gameState.getLastMove() : null;
        Move nextLastMove = newState != null ? newState.getLastMove() : null;
        boolean shouldAnimate = hasRenderedInitialState
                && nextLastMove != null
                && !Objects.equals(previousLastMove, nextLastMove);

        this.gameState = newState;
        this.hasRenderedInitialState = newState != null;
        this.allLegalMoves = (newState != null && !newState.isGameOver())
                ? newState.generateMoves()
                : List.of();

        // Keep selection only if still valid for the active side
        if (selectedSquare != null) {
            updateLegalMovesFromSelected(selectedSquare);
            if (legalMovesFromSelected.isEmpty() && !hasFriendlyPieceAt(selectedSquare)) {
                selectedSquare = null;
            }
        }

        if (shouldAnimate) {
            startMoveAnimation(nextLastMove);
        } else {
            cancelMoveAnimation();
            invalidate();
        }
    }

    @Nullable
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Sets the currently selected square and updates legal destination highlights.
     */
    public void setSelectedSquare(@Nullable Square square) {
        if (Objects.equals(this.selectedSquare, square)) {
            return;
        }
        this.selectedSquare = square;
        updateLegalMovesFromSelected(square);
        if (onSquareSelectedListener != null) {
            onSquareSelectedListener.onSquareSelected(square);
        }
        invalidate();
    }

    @Nullable
    public Square getSelectedSquare() {
        return selectedSquare;
    }

    /**
     * Enables or disables rotating Black's pieces 180 degrees for pass-and-play on one device.
     */
    public void setRotateBlackPieces(boolean rotateBlackPieces) {
        if (this.rotateBlackPieces != rotateBlackPieces) {
            this.rotateBlackPieces = rotateBlackPieces;
            invalidate();
        }
    }

    public boolean isRotateBlackPieces() {
        return rotateBlackPieces;
    }

    /**
     * Alias setter supporting the "flipped" / "rotate black pieces 180°" option.
     */
    public void setFlipped(boolean flipped) {
        setRotateBlackPieces(flipped);
    }

    public boolean isFlipped() {
        return rotateBlackPieces;
    }

    /**
     * Optional perspective flip (places Black at the bottom of the screen if true).
     */
    public void setBoardPerspectiveFlipped(boolean boardPerspectiveFlipped) {
        if (this.boardPerspectiveFlipped != boardPerspectiveFlipped) {
            this.boardPerspectiveFlipped = boardPerspectiveFlipped;
            invalidate();
        }
    }

    public boolean isBoardPerspectiveFlipped() {
        return boardPerspectiveFlipped;
    }

    public void setInteractionEnabled(boolean interactionEnabled) {
        this.interactionEnabled = interactionEnabled;
    }

    public boolean isInteractionEnabled() {
        return interactionEnabled;
    }

    public void setOnMoveSelectedListener(@Nullable OnMoveSelectedListener listener) {
        this.onMoveSelectedListener = listener;
    }

    public void setOnSquareSelectedListener(@Nullable OnSquareSelectedListener listener) {
        this.onSquareSelectedListener = listener;
    }

    public void setOnPromotionRequestedListener(@Nullable OnPromotionRequestedListener listener) {
        this.onPromotionRequestedListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int size;
        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            size = dpToPx(DEFAULT_BOARD_DP);
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            size = heightSize;
        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
            size = widthSize;
        } else {
            size = Math.min(widthSize, heightSize);
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int boardPx = Math.min(getWidth(), getHeight());
        if (boardPx <= 0) {
            return;
        }

        if (demoConfig != null) {
            drawDemoBoard(canvas, demoConfig, boardPx);
            return;
        }

        float squareSize = boardPx / (float) Square.BOARD_SIZE;

        // 1. Draw 8x8 squares
        for (int rank = 0; rank < Square.BOARD_SIZE; rank++) {
            for (int file = 0; file < Square.BOARD_SIZE; file++) {
                boolean isLightSquare = ((rank + file) % 2) != 0;
                getSquareBounds(rank, file, squareSize, squareRect);
                canvas.drawRect(squareRect, isLightSquare ? lightSquarePaint : darkSquarePaint);
            }
        }

        // 2. Draw last-move highlight
        Move lastMove = gameState != null ? gameState.getLastMove() : null;
        if (lastMove != null) {
            drawSquareHighlight(canvas, lastMove.from(), squareSize, lastMovePaint);
            drawSquareHighlight(canvas, lastMove.to(), squareSize, lastMovePaint);
        }

        // 3. Draw selected square highlight
        if (selectedSquare != null) {
            drawSquareHighlight(canvas, selectedSquare, squareSize, selectedSquarePaint);
        }

        // 4. Draw pieces (static and animated)
        if (gameState != null) {
            drawBoardPieces(canvas, gameState.getBoard(), squareSize);
        }

        // 5. Draw legal destination indicators (dots for empty squares, rings for capture squares)
        drawLegalMoveIndicators(canvas, squareSize);

        // 6. Subtle outer frame border
        drawOuterBorder(canvas, boardPx);
    }

    private void drawDemoBoard(@NonNull Canvas canvas,
                               @NonNull BoardDemoConfig config,
                               int boardPx) {
        int gridSize = config.getGridSize();
        int startRank = config.getStartRank();
        int startFile = config.getStartFile();
        float squareSize = boardPx / (float) gridSize;

        // 1. Draw visible grid squares (5x5 or 8x8)
        for (int rOff = 0; rOff < gridSize; rOff++) {
            int rank = startRank + rOff;
            for (int fOff = 0; fOff < gridSize; fOff++) {
                int file = startFile + fOff;
                boolean isLightSquare = ((rank + file) % 2) != 0;
                getDemoSquareBounds(config, rank, file, squareSize, squareRect);
                canvas.drawRect(squareRect, isLightSquare ? lightSquarePaint : darkSquarePaint);
            }
        }

        // 2. Draw gentle pulsing highlights on key squares
        int pulseAlpha = Math.max(25, Math.min(255, Math.round(baseHighlightAlpha * demoPulseFactor)));
        demoPulsePaint.setAlpha(pulseAlpha);
        for (Square sq : config.getPulsingSquares()) {
            if (isSquareVisibleInDemo(config, sq)) {
                getDemoSquareBounds(config, sq.rank(), sq.file(), squareSize, squareRect);
                canvas.drawRect(squareRect, demoPulsePaint);
            }
        }

        // 3. Draw pieces inside the visible demo window
        Board board = config.getBoard();
        for (int rOff = 0; rOff < gridSize; rOff++) {
            int rank = startRank + rOff;
            for (int fOff = 0; fOff < gridSize; fOff++) {
                int file = startFile + fOff;
                Piece piece = board.getPieceAt(rank, file);
                if (piece != null) {
                    getDemoSquareBounds(config, rank, file, squareSize, squareRect);
                    drawPieceInRect(canvas, piece, squareRect);
                }
            }
        }

        // 4. Draw move dots and capture rings
        float dotRadius = squareSize * 0.16f;
        float ringStrokeWidth = squareSize * 0.085f;
        float ringRadius = (squareSize * 0.43f) - (ringStrokeWidth / 2f);
        captureRingPaint.setStrokeWidth(ringStrokeWidth);

        for (Square dotSq : config.getMoveDotSquares()) {
            if (isSquareVisibleInDemo(config, dotSq)) {
                getDemoSquareBounds(config, dotSq.rank(), dotSq.file(), squareSize, squareRect);
                canvas.drawCircle(squareRect.centerX(), squareRect.centerY(), dotRadius, moveDotPaint);
            }
        }
        for (Square ringSq : config.getCaptureRingSquares()) {
            if (isSquareVisibleInDemo(config, ringSq)) {
                getDemoSquareBounds(config, ringSq.rank(), ringSq.file(), squareSize, squareRect);
                canvas.drawCircle(squareRect.centerX(), squareRect.centerY(), ringRadius, captureRingPaint);
            }
        }

        // 5. Draw directional arrows
        for (BoardDemoConfig.Arrow arrow : config.getArrows()) {
            if (isSquareVisibleInDemo(config, arrow.from()) && isSquareVisibleInDemo(config, arrow.to())) {
                drawDemoArrow(canvas, config, arrow.from(), arrow.to(), squareSize);
            }
        }

        // 6. Outer border
        drawOuterBorder(canvas, boardPx);
    }

    private void drawDemoArrow(@NonNull Canvas canvas,
                               @NonNull BoardDemoConfig config,
                               @NonNull Square from,
                               @NonNull Square to,
                               float squareSize) {
        getDemoSquareBounds(config, from.rank(), from.file(), squareSize, squareRect);
        float startX = squareRect.centerX();
        float startY = squareRect.centerY();

        getDemoSquareBounds(config, to.rank(), to.file(), squareSize, squareRect);
        float endX = squareRect.centerX();
        float endY = squareRect.centerY();

        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.hypot(dx, dy);
        if (length < 1f) {
            return;
        }

        float ux = dx / length;
        float uy = dy / length;

        float startInset = squareSize * 0.24f;
        float headLength = squareSize * 0.28f;
        float headHalfWidth = squareSize * 0.16f;
        float endInset = squareSize * 0.16f;

        float lineStartX = startX + ux * startInset;
        float lineStartY = startY + uy * startInset;
        float tipX = endX - ux * endInset;
        float tipY = endY - uy * endInset;
        float baseX = tipX - ux * headLength;
        float baseY = tipY - uy * headLength;

        demoArrowLinePaint.setStrokeWidth(Math.max(dpToPx(2), squareSize * 0.09f));
        canvas.drawLine(lineStartX, lineStartY, baseX, baseY, demoArrowLinePaint);

        float perpX = -uy;
        float perpY = ux;
        arrowHeadPath.reset();
        arrowHeadPath.moveTo(tipX, tipY);
        arrowHeadPath.lineTo(baseX + perpX * headHalfWidth, baseY + perpY * headHalfWidth);
        arrowHeadPath.lineTo(baseX - perpX * headHalfWidth, baseY - perpY * headHalfWidth);
        arrowHeadPath.close();
        canvas.drawPath(arrowHeadPath, demoArrowHeadPaint);
    }

    private boolean isSquareVisibleInDemo(@NonNull BoardDemoConfig config, @NonNull Square sq) {
        int r = sq.rank() - config.getStartRank();
        int f = sq.file() - config.getStartFile();
        return r >= 0 && r < config.getGridSize() && f >= 0 && f < config.getGridSize();
    }

    private void getDemoSquareBounds(@NonNull BoardDemoConfig config,
                                     int rank,
                                     int file,
                                     float squareSize,
                                     @NonNull RectF outRect) {
        int col = file - config.getStartFile();
        int row = (config.getGridSize() - 1) - (rank - config.getStartRank());
        float left = col * squareSize;
        float top = row * squareSize;
        outRect.set(left, top, left + squareSize, top + squareSize);
    }

    private void drawOuterBorder(@NonNull Canvas canvas, int boardPx) {
        float halfStroke = boardBorderPaint.getStrokeWidth() / 2f;
        canvas.drawRect(
                halfStroke,
                halfStroke,
                boardPx - halfStroke,
                boardPx - halfStroke,
                boardBorderPaint
        );
    }

    private void drawSquareHighlight(@NonNull Canvas canvas,
                                     @NonNull Square square,
                                     float squareSize,
                                     @NonNull Paint paint) {
        getSquareBounds(square.rank(), square.file(), squareSize, squareRect);
        canvas.drawRect(squareRect, paint);
    }

    private void drawBoardPieces(@NonNull Canvas canvas,
                                 @NonNull Board board,
                                 float squareSize) {
        boolean isAnimating = animatingMove != null && animationProgress < 1f;
        Square animDest = isAnimating ? animatingMove.to() : null;

        // If castling is animating, also animate the rook from its corner to its castled square
        Square castleRookFrom = null;
        Square castleRookTo = null;
        if (isAnimating && animatingMove.isCastle()) {
            int rank = animatingMove.from().rank();
            if (animatingMove.to().file() == 6) {
                castleRookFrom = Square.of(rank, 7);
                castleRookTo = Square.of(rank, 5);
            } else if (animatingMove.to().file() == 2) {
                castleRookFrom = Square.of(rank, 0);
                castleRookTo = Square.of(rank, 3);
            }
        }

        for (int rank = 0; rank < Square.BOARD_SIZE; rank++) {
            for (int file = 0; file < Square.BOARD_SIZE; file++) {
                Piece piece = board.getPieceAt(rank, file);
                if (piece == null) {
                    continue;
                }
                Square currentSq = Square.of(rank, file);
                if (currentSq.equals(animDest) || currentSq.equals(castleRookTo)) {
                    continue;
                }
                getSquareBounds(rank, file, squareSize, squareRect);
                drawPieceInRect(canvas, piece, squareRect);
            }
        }

        if (isAnimating && animDest != null) {
            Piece movedPiece = board.getPieceAt(animDest);
            if (movedPiece != null) {
                drawAnimatedPiece(canvas, movedPiece, animatingMove.from(), animDest, squareSize);
            }
            if (castleRookFrom != null && castleRookTo != null) {
                Piece rookPiece = board.getPieceAt(castleRookTo);
                if (rookPiece != null) {
                    drawAnimatedPiece(canvas, rookPiece, castleRookFrom, castleRookTo, squareSize);
                }
            }
        }
    }

    private void drawAnimatedPiece(@NonNull Canvas canvas,
                                   @NonNull Piece piece,
                                   @NonNull Square from,
                                   @NonNull Square to,
                                   float squareSize) {
        RectF fromBounds = new RectF();
        RectF toBounds = new RectF();
        getSquareBounds(from.rank(), from.file(), squareSize, fromBounds);
        getSquareBounds(to.rank(), to.file(), squareSize, toBounds);

        float left = fromBounds.left + (toBounds.left - fromBounds.left) * animationProgress;
        float top = fromBounds.top + (toBounds.top - fromBounds.top) * animationProgress;
        squareRect.set(left, top, left + squareSize, top + squareSize);
        drawPieceInRect(canvas, piece, squareRect);
    }

    private void drawPieceInRect(@NonNull Canvas canvas,
                                 @NonNull Piece piece,
                                 @NonNull RectF cellBounds) {
        Map<PieceType, Drawable> byType = drawableCache.get(piece.color());
        if (byType == null) {
            return;
        }
        Drawable drawable = byType.get(piece.type());
        if (drawable == null) {
            return;
        }

        float pad = cellBounds.width() * 0.08f;
        int left = Math.round(cellBounds.left + pad);
        int top = Math.round(cellBounds.top + pad);
        int right = Math.round(cellBounds.right - pad);
        int bottom = Math.round(cellBounds.bottom - pad);
        drawable.setBounds(left, top, right, bottom);

        boolean shouldRotate = rotateBlackPieces && piece.color() == Color.BLACK;
        if (shouldRotate) {
            canvas.save();
            canvas.rotate(180f, cellBounds.centerX(), cellBounds.centerY());
            drawable.draw(canvas);
            canvas.restore();
        } else {
            drawable.draw(canvas);
        }
    }

    private void drawLegalMoveIndicators(@NonNull Canvas canvas, float squareSize) {
        if (selectedSquare == null || legalMovesFromSelected.isEmpty()) {
            return;
        }

        float dotRadius = squareSize * 0.16f;
        float ringStrokeWidth = squareSize * 0.085f;
        float ringRadius = (squareSize * 0.43f) - (ringStrokeWidth / 2f);
        captureRingPaint.setStrokeWidth(ringStrokeWidth);

        Set<Square> drawnDestinations = new HashSet<>();
        for (Move move : legalMovesFromSelected) {
            Square dest = move.to();
            if (!drawnDestinations.add(dest)) {
                continue;
            }
            getSquareBounds(dest.rank(), dest.file(), squareSize, squareRect);
            float cx = squareRect.centerX();
            float cy = squareRect.centerY();

            if (move.isCapture()) {
                canvas.drawCircle(cx, cy, ringRadius, captureRingPaint);
            } else {
                canvas.drawCircle(cx, cy, dotRadius, moveDotPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (demoConfig != null || !interactionEnabled || gameState == null || gameState.isGameOver()) {
            return false;
        }

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            Square tapped = resolveSquareAt(event.getX(), event.getY());
            if (tapped != null) {
                handleSquareTap(tapped);
                performClick();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void handleSquareTap(@NonNull Square tappedSquare) {
        if (gameState == null) {
            return;
        }

        // 1. Check if tappedSquare is a legal destination for the currently selected square
        if (selectedSquare != null && !legalMovesFromSelected.isEmpty()) {
            List<Move> matchingMoves = new ArrayList<>();
            for (Move move : legalMovesFromSelected) {
                if (move.to().equals(tappedSquare)) {
                    matchingMoves.add(move);
                }
            }

            if (!matchingMoves.isEmpty()) {
                if (matchingMoves.get(0).isPromotion() || matchingMoves.size() > 1) {
                    requestPromotionChoice(gameState.getSideToMove(), matchingMoves);
                } else {
                    Move chosenMove = matchingMoves.get(0);
                    setSelectedSquare(null);
                    dispatchMoveSelected(chosenMove);
                }
                return;
            }
        }

        // 2. Otherwise, toggle or switch selection to a friendly piece of the side to move
        if (tappedSquare.equals(selectedSquare)) {
            setSelectedSquare(null);
        } else if (hasFriendlyPieceAt(tappedSquare)) {
            setSelectedSquare(tappedSquare);
        } else {
            setSelectedSquare(null);
        }
    }

    private void requestPromotionChoice(@NonNull Color sideToMove,
                                        @NonNull List<Move> promotionMoves) {
        if (onPromotionRequestedListener != null) {
            onPromotionRequestedListener.onPromotionRequested(sideToMove, promotionMoves);
        } else {
            PromotionDialogHelper.show(
                    getContext(),
                    sideToMove,
                    promotionMoves,
                    new PromotionDialogHelper.PromotionSelectionCallback() {
                        @Override
                        public void onPromotionMoveSelected(@NonNull Move chosenMove) {
                            setSelectedSquare(null);
                            dispatchMoveSelected(chosenMove);
                        }
                    }
            );
        }
    }

    private void dispatchMoveSelected(@NonNull Move move) {
        if (onMoveSelectedListener != null) {
            onMoveSelectedListener.onMoveSelected(move);
        }
    }

    private boolean hasFriendlyPieceAt(@Nullable Square square) {
        if (square == null || gameState == null) {
            return false;
        }
        Piece piece = gameState.getBoard().getPieceAt(square);
        return piece != null && piece.color() == gameState.getSideToMove();
    }

    private void updateLegalMovesFromSelected(@Nullable Square origin) {
        if (origin == null || allLegalMoves.isEmpty()) {
            this.legalMovesFromSelected = List.of();
            return;
        }
        List<Move> filtered = new ArrayList<>();
        for (Move move : allLegalMoves) {
            if (move.from().equals(origin)) {
                filtered.add(move);
            }
        }
        this.legalMovesFromSelected = filtered;
    }

    private void getSquareBounds(int rank, int file, float squareSize, @NonNull RectF outRect) {
        int col = boardPerspectiveFlipped ? (Square.BOARD_SIZE - 1 - file) : file;
        int row = boardPerspectiveFlipped ? rank : (Square.BOARD_SIZE - 1 - rank);
        float left = col * squareSize;
        float top = row * squareSize;
        outRect.set(left, top, left + squareSize, top + squareSize);
    }

    @Nullable
    private Square resolveSquareAt(float x, float y) {
        int boardPx = Math.min(getWidth(), getHeight());
        if (boardPx <= 0 || x < 0 || y < 0 || x >= boardPx || y >= boardPx) {
            return null;
        }
        float squareSize = boardPx / (float) Square.BOARD_SIZE;
        int col = (int) (x / squareSize);
        int row = (int) (y / squareSize);
        if (col < 0 || col >= Square.BOARD_SIZE || row < 0 || row >= Square.BOARD_SIZE) {
            return null;
        }
        int file = boardPerspectiveFlipped ? (Square.BOARD_SIZE - 1 - col) : col;
        int rank = boardPerspectiveFlipped ? row : (Square.BOARD_SIZE - 1 - row);
        return Square.of(rank, file);
    }

    private void startMoveAnimation(@NonNull Move move) {
        cancelMoveAnimation();
        this.animatingMove = move;
        this.animationProgress = 0f;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(MOVE_ANIMATION_DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(anim -> {
            animationProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatingMove = null;
                animationProgress = 1f;
                invalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animatingMove = null;
                animationProgress = 1f;
                invalidate();
            }
        });
        this.moveAnimator = animator;
        animator.start();
    }

    private void cancelMoveAnimation() {
        if (moveAnimator != null) {
            moveAnimator.cancel();
            moveAnimator = null;
        }
        animatingMove = null;
        animationProgress = 1f;
    }

    private void updateDemoPulseAnimation() {
        cancelDemoPulseAnimation();
        if (!isAttachedToWindow() || demoConfig == null || demoConfig.getPulsingSquares().isEmpty()) {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0.45f, 1.15f);
        animator.setDuration(DEMO_PULSE_DURATION_MS);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(anim -> {
            demoPulseFactor = (float) anim.getAnimatedValue();
            invalidate();
        });
        this.demoPulseAnimator = animator;
        animator.start();
    }

    private void cancelDemoPulseAnimation() {
        if (demoPulseAnimator != null) {
            demoPulseAnimator.cancel();
            demoPulseAnimator = null;
        }
        demoPulseFactor = 0.85f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateDemoPulseAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelMoveAnimation();
        cancelDemoPulseAnimation();
        super.onDetachedFromWindow();
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }
}
