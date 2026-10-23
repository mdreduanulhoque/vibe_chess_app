package com.reduan.vibechess.ui.game;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.reduan.vibechess.core.AiDifficulty;
import com.reduan.vibechess.core.AiMoveSource;
import com.reduan.vibechess.core.ChessClock;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.GameResult;
import com.reduan.vibechess.core.GameState;
import com.reduan.vibechess.core.GameStatus;
import com.reduan.vibechess.core.LocalHumanMoveSource;
import com.reduan.vibechess.core.Move;
import com.reduan.vibechess.core.MoveSource;
import com.reduan.vibechess.core.Square;
import com.reduan.vibechess.data.SettingsPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ViewModel holding the active {@link GameState} from {@code :game-core}, driving the chess clock
 * via {@link SystemClock#elapsedRealtime()} deltas (~10x/sec on the main thread), managing pause/resume,
 * emitting sound/haptic events, persisting state into {@link SavedStateHandle} across process death,
 * and coordinating turns between White and Black {@link MoveSource} instances
 * ({@link LocalHumanMoveSource} and {@link AiMoveSource}).
 * Survives configuration changes (rotation) and process death without clock drift or leaks.
 */
public class GameViewModel extends AndroidViewModel {

    public enum SoundEvent {
        MOVE,
        CAPTURE,
        LOW_TIME_TICK,
        GAME_END
    }

    private static final long CLOCK_UPDATE_INTERVAL_MS = 100L;
    private static final long LOW_TIME_THRESHOLD_MS = 10_000L;

    static final String KEY_INITIALIZED = "vm_initialized";
    static final String KEY_SINGLE_PLAYER = "vm_single_player";
    static final String KEY_HUMAN_COLOR = "vm_human_color";
    static final String KEY_AI_DIFFICULTY = "vm_ai_difficulty";
    static final String KEY_RANDOM_COLOR_CHOICE = "vm_random_color_choice";
    static final String KEY_GAME_STATE = "vm_game_state";
    static final String KEY_WHITE_REMAINING_MS = "vm_white_remaining_ms";
    static final String KEY_BLACK_REMAINING_MS = "vm_black_remaining_ms";
    static final String KEY_PAUSED = "vm_paused";
    static final String KEY_SELECTED_SQUARE = "vm_selected_square";
    static final String KEY_PENDING_PROMO_FROM = "vm_pending_promo_from";
    static final String KEY_PENDING_PROMO_TO = "vm_pending_promo_to";
    static final String KEY_RESULT_NAVIGATED = "vm_result_navigated";

    private final SavedStateHandle savedStateHandle;
    private final SettingsPreferences settingsPreferences;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MoveSource whiteMoveSource = new LocalHumanMoveSource();
    private MoveSource blackMoveSource = new LocalHumanMoveSource();

    private boolean singlePlayer = false;
    private Color humanColor = Color.WHITE;
    private AiDifficulty aiDifficulty = AiDifficulty.MEDIUM;
    private boolean randomColorChoice = false;

    private final MutableLiveData<GameState> gameStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Square> selectedSquareLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<List<Move>> pendingPromotionMovesLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> rotateBlackPiecesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pausedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> aiThinkingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Color> skippedTurnNoticeLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<SoundEvent> soundEventLiveData = new MutableLiveData<>(null);

    private boolean initialized = false;
    private boolean clockRunning = false;
    private long lastTickElapsedRealtime = 0L;
    private boolean resultNavigated = false;

    private final Runnable clockTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!clockRunning) {
                return;
            }
            applyElapsedRealtimeDelta();
            GameState current = gameStateLiveData.getValue();
            if (current != null && !current.isGameOver() && clockRunning) {
                mainHandler.postDelayed(this, CLOCK_UPDATE_INTERVAL_MS);
            }
        }
    };

    public GameViewModel(@NonNull Application application) {
        this(application, new SavedStateHandle());
    }

    public GameViewModel(@NonNull Application application, @NonNull SavedStateHandle savedStateHandle) {
        super(application);
        this.savedStateHandle = Objects.requireNonNull(savedStateHandle, "savedStateHandle must not be null");
        this.settingsPreferences = new SettingsPreferences(application);
        this.rotateBlackPiecesLiveData.setValue(
                settingsPreferences.isRotateOpponentPiecesEnabled()
        );
    }

    /**
     * Initializes a Multiplayer (pass-and-play) game with the given clock duration per side.
     */
    public void initIfNeeded(long initialClockMillis) {
        initIfNeeded(initialClockMillis, false, Color.WHITE, AiDifficulty.MEDIUM, false);
    }

    /**
     * Initializes a Single Player or Multiplayer game if not already initialized.
     * Safe to call from {@code Fragment.onViewCreated} across screen rotations and process death.
     */
    public void initIfNeeded(long initialClockMillis,
                             boolean singlePlayer,
                             @NonNull Color humanColor,
                             @NonNull AiDifficulty difficulty,
                             boolean randomColorChoice) {
        if (initialized) {
            return;
        }
        initialized = true;

        if (restoreFromSavedStateIfAvailable()) {
            return;
        }

        this.singlePlayer = singlePlayer;
        this.humanColor = Objects.requireNonNull(humanColor, "humanColor must not be null");
        this.aiDifficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.randomColorChoice = randomColorChoice;

        configureMoveSources();

        long validMillis = initialClockMillis > 0 ? initialClockMillis : ChessClock.ONE_MINUTE_MILLIS;
        GameState initialState = GameState.initial(validMillis);
        gameStateLiveData.setValue(initialState);
        persistSetupMetadata();
        persistGameStateSnapshot(initialState);

        startClockLoop();
        requestMoveFromActiveSource(initialState);
    }

    private boolean restoreFromSavedStateIfAvailable() {
        Boolean wasInitialized = savedStateHandle.get(KEY_INITIALIZED);
        String encodedState = savedStateHandle.get(KEY_GAME_STATE);
        if (!Boolean.TRUE.equals(wasInitialized) || encodedState == null) {
            return false;
        }

        try {
            GameState baseState = GameState.fromCompactString(encodedState);
            Long savedWhiteMs = savedStateHandle.get(KEY_WHITE_REMAINING_MS);
            Long savedBlackMs = savedStateHandle.get(KEY_BLACK_REMAINING_MS);

            ChessClock baseClock = baseState.getClock();
            long whiteMs = savedWhiteMs != null ? savedWhiteMs : baseClock.getWhiteRemainingMillis();
            long blackMs = savedBlackMs != null ? savedBlackMs : baseClock.getBlackRemainingMillis();
            ChessClock restoredClock = new ChessClock(
                    baseClock.getInitialMillis(),
                    whiteMs,
                    blackMs,
                    baseClock.getActiveSide()
            );

            GameStatus status = restoredClock.isFlagged() ? GameStatus.GAME_OVER : baseState.getStatus();
            GameResult result = restoredClock.isFlagged()
                    ? GameResult.fromPointsAtTimeout(
                            baseState.points(Color.WHITE),
                            baseState.points(Color.BLACK)
                    )
                    : baseState.getResult();

            GameState restoredState = new GameState(
                    baseState.getBoard(),
                    baseState.getSideToMove(),
                    baseState.getCastlingRights(),
                    baseState.getEnPassantTarget(),
                    restoredClock,
                    status,
                    result,
                    baseState.getCapturedPieces(),
                    baseState.getLastMove()
            );

            Boolean sp = savedStateHandle.get(KEY_SINGLE_PLAYER);
            this.singlePlayer = Boolean.TRUE.equals(sp);

            String colorStr = savedStateHandle.get(KEY_HUMAN_COLOR);
            this.humanColor = colorStr != null ? Color.valueOf(colorStr) : Color.WHITE;

            String diffStr = savedStateHandle.get(KEY_AI_DIFFICULTY);
            this.aiDifficulty = diffStr != null ? AiDifficulty.valueOf(diffStr) : AiDifficulty.MEDIUM;

            Boolean randChoice = savedStateHandle.get(KEY_RANDOM_COLOR_CHOICE);
            this.randomColorChoice = Boolean.TRUE.equals(randChoice);

            Boolean nav = savedStateHandle.get(KEY_RESULT_NAVIGATED);
            this.resultNavigated = Boolean.TRUE.equals(nav);

            Boolean wasPaused = savedStateHandle.get(KEY_PAUSED);
            boolean restoredPaused = Boolean.TRUE.equals(wasPaused) && !restoredState.isGameOver();
            this.pausedLiveData.setValue(restoredPaused);

            String selSqStr = savedStateHandle.get(KEY_SELECTED_SQUARE);
            if (selSqStr != null) {
                try {
                    this.selectedSquareLiveData.setValue(Square.fromAlgebraic(selSqStr));
                } catch (IllegalArgumentException ignored) {
                    this.selectedSquareLiveData.setValue(null);
                }
            }

            String promoFromStr = savedStateHandle.get(KEY_PENDING_PROMO_FROM);
            String promoToStr = savedStateHandle.get(KEY_PENDING_PROMO_TO);
            if (promoFromStr != null && promoToStr != null && !restoredState.isGameOver()) {
                try {
                    Square promoFrom = Square.fromAlgebraic(promoFromStr);
                    Square promoTo = Square.fromAlgebraic(promoToStr);
                    List<Move> matchingPromos = new ArrayList<>();
                    for (Move m : restoredState.generateMoves()) {
                        if (m.from().equals(promoFrom) && m.to().equals(promoTo) && m.isPromotion()) {
                            matchingPromos.add(m);
                        }
                    }
                    if (!matchingPromos.isEmpty()) {
                        this.pendingPromotionMovesLiveData.setValue(List.copyOf(matchingPromos));
                    }
                } catch (IllegalArgumentException ignored) {
                    this.pendingPromotionMovesLiveData.setValue(null);
                }
            }

            configureMoveSources();
            gameStateLiveData.setValue(restoredState);

            if (!restoredState.isGameOver() && !restoredPaused) {
                startClockLoop();
                requestMoveFromActiveSource(restoredState);
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void configureMoveSources() {
        shutdownMoveSources();
        if (singlePlayer) {
            AiMoveSource aiSource = new AiMoveSource(
                    aiDifficulty,
                    mainHandler::post,
                    aiThinkingLiveData::setValue
            );
            LocalHumanMoveSource humanSource = new LocalHumanMoveSource();
            if (humanColor == Color.WHITE) {
                this.whiteMoveSource = humanSource;
                this.blackMoveSource = aiSource;
            } else {
                this.whiteMoveSource = aiSource;
                this.blackMoveSource = humanSource;
            }
        } else {
            this.whiteMoveSource = new LocalHumanMoveSource();
            this.blackMoveSource = new LocalHumanMoveSource();
        }
    }

    private void persistSetupMetadata() {
        savedStateHandle.set(KEY_INITIALIZED, true);
        savedStateHandle.set(KEY_SINGLE_PLAYER, singlePlayer);
        savedStateHandle.set(KEY_HUMAN_COLOR, humanColor.name());
        savedStateHandle.set(KEY_AI_DIFFICULTY, aiDifficulty.name());
        savedStateHandle.set(KEY_RANDOM_COLOR_CHOICE, randomColorChoice);
    }

    private void persistGameStateSnapshot(@NonNull GameState state) {
        savedStateHandle.set(KEY_GAME_STATE, state.toCompactString());
        persistClockMillis(state);
    }

    private void persistClockMillis(@NonNull GameState state) {
        ChessClock clock = state.getClock();
        savedStateHandle.set(KEY_WHITE_REMAINING_MS, clock.getWhiteRemainingMillis());
        savedStateHandle.set(KEY_BLACK_REMAINING_MS, clock.getBlackRemainingMillis());
    }

    public boolean isSinglePlayer() {
        return singlePlayer;
    }

    @NonNull
    public Color getHumanColor() {
        return humanColor;
    }

    @NonNull
    public AiDifficulty getAiDifficulty() {
        return aiDifficulty;
    }

    public boolean isRandomColorChoice() {
        return randomColorChoice;
    }

    @NonNull
    public LiveData<GameState> getGameState() {
        return gameStateLiveData;
    }

    @NonNull
    public LiveData<Square> getSelectedSquare() {
        return selectedSquareLiveData;
    }

    public void setSelectedSquare(@Nullable Square square) {
        if (Boolean.TRUE.equals(pausedLiveData.getValue())) {
            return;
        }
        if (!Objects.equals(selectedSquareLiveData.getValue(), square)) {
            selectedSquareLiveData.setValue(square);
            savedStateHandle.set(KEY_SELECTED_SQUARE, square != null ? square.toAlgebraic() : null);
        }
    }

    @NonNull
    public LiveData<List<Move>> getPendingPromotionMoves() {
        return pendingPromotionMovesLiveData;
    }

    public void requestPromotionChoice(@NonNull List<Move> promotionMoves) {
        if (Boolean.TRUE.equals(pausedLiveData.getValue()) || promotionMoves.isEmpty()) {
            return;
        }
        List<Move> copy = List.copyOf(promotionMoves);
        pendingPromotionMovesLiveData.setValue(copy);
        Move first = copy.get(0);
        savedStateHandle.set(KEY_PENDING_PROMO_FROM, first.from().toAlgebraic());
        savedStateHandle.set(KEY_PENDING_PROMO_TO, first.to().toAlgebraic());
    }

    public void cancelPendingPromotion() {
        pendingPromotionMovesLiveData.setValue(null);
        savedStateHandle.set(KEY_PENDING_PROMO_FROM, null);
        savedStateHandle.set(KEY_PENDING_PROMO_TO, null);
    }

    @NonNull
    public LiveData<Boolean> getRotateBlackPieces() {
        return rotateBlackPiecesLiveData;
    }

    public void setRotateBlackPieces(boolean rotate) {
        settingsPreferences.setRotateOpponentPiecesEnabled(rotate);
        if (!Objects.equals(rotateBlackPiecesLiveData.getValue(), rotate)) {
            rotateBlackPiecesLiveData.setValue(rotate);
        }
    }

    @NonNull
    public LiveData<Boolean> getPaused() {
        return pausedLiveData;
    }

    public boolean isPaused() {
        return Boolean.TRUE.equals(pausedLiveData.getValue());
    }

    @NonNull
    public LiveData<Boolean> getAiThinking() {
        return aiThinkingLiveData;
    }

    @NonNull
    public LiveData<SoundEvent> getSoundEvent() {
        return soundEventLiveData;
    }

    public void consumeSoundEvent() {
        soundEventLiveData.setValue(null);
    }

    /**
     * Pauses the active game clock, cancels any running AI computation cleanly,
     * and hides the board behind the pause overlay.
     */
    public void pauseGame() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver() || isPaused()) {
            return;
        }
        applyElapsedRealtimeDelta();
        stopClockLoop();
        whiteMoveSource.cancel();
        blackMoveSource.cancel();
        aiThinkingLiveData.setValue(false);

        GameState afterTick = gameStateLiveData.getValue();
        if (afterTick != null && afterTick.isGameOver()) {
            return;
        }
        pausedLiveData.setValue(true);
        savedStateHandle.set(KEY_PAUSED, true);
        if (afterTick != null) {
            persistGameStateSnapshot(afterTick);
        }
    }

    /**
     * Resumes the game clock from pause and re-requests a move from the active side's MoveSource.
     */
    public void resumeGame() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver() || !isPaused()) {
            return;
        }
        pausedLiveData.setValue(false);
        savedStateHandle.set(KEY_PAUSED, false);
        startClockLoop();
        requestMoveFromActiveSource(current);
    }

    /**
     * Cancels active clocks and background AI tasks cleanly when the player exits the game screen.
     */
    public void onExitGame() {
        stopClockLoop();
        shutdownMoveSources();
        aiThinkingLiveData.setValue(false);
    }

    @NonNull
    public LiveData<Color> getSkippedTurnNotice() {
        return skippedTurnNoticeLiveData;
    }

    public void consumeSkippedTurnNotice() {
        skippedTurnNoticeLiveData.setValue(null);
    }

    public boolean markResultNavigated() {
        if (resultNavigated) {
            return false;
        }
        resultNavigated = true;
        savedStateHandle.set(KEY_RESULT_NAVIGATED, true);
        return true;
    }

    /**
     * Returns true if the current side to move is controlled by a local human on this device
     * and the game is active and unpaused.
     */
    public boolean isCurrentTurnHumanInteractive() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver() || isPaused()) {
            return false;
        }
        return getMoveSourceFor(current.getSideToMove()).isHumanInteractive();
    }

    /**
     * Submits a move selected by the local human player on the board UI.
     */
    public void onHumanMoveSelected(@NonNull Move move) {
        if (isPaused()) {
            return;
        }
        applyElapsedRealtimeDelta();
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver()) {
            return;
        }
        MoveSource activeSource = getMoveSourceFor(current.getSideToMove());
        if (activeSource instanceof LocalHumanMoveSource humanSource) {
            humanSource.submitMove(move);
        }
    }

    private void onMoveChosenBySource(@NonNull Move move) {
        if (isPaused()) {
            return;
        }
        applyElapsedRealtimeDelta();
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver()) {
            return;
        }

        selectedSquareLiveData.setValue(null);
        savedStateHandle.set(KEY_SELECTED_SQUARE, null);
        cancelPendingPromotion();

        GameState nextState = current.applyMove(move);
        soundEventLiveData.setValue(move.isCapture() ? SoundEvent.CAPTURE : SoundEvent.MOVE);

        // Per PROJECT_SPEC.md: if a side has no legal move, its turn is skipped and its clock keeps running.
        if (!nextState.isGameOver() && !nextState.hasLegalMoves()) {
            Color skippedSide = nextState.getSideToMove();
            nextState = nextState.skipTurn();
            skippedTurnNoticeLiveData.setValue(skippedSide);
        }

        lastTickElapsedRealtime = SystemClock.elapsedRealtime();
        gameStateLiveData.setValue(nextState);
        persistGameStateSnapshot(nextState);

        if (nextState.isGameOver()) {
            onGameEnded();
        } else if (nextState.hasLegalMoves()) {
            requestMoveFromActiveSource(nextState);
        }
    }

    private void applyElapsedRealtimeDelta() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver()) {
            stopClockLoop();
            return;
        }
        if (!clockRunning || lastTickElapsedRealtime <= 0L) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long delta = Math.max(0L, now - lastTickElapsedRealtime);
        lastTickElapsedRealtime = now;
        if (delta <= 0L) {
            return;
        }

        Color activeSide = current.getClock().getActiveSide();
        long prevRemaining = current.getClock().getRemainingMillis(activeSide);

        GameState ticked = current.tick(delta);
        gameStateLiveData.setValue(ticked);
        persistClockMillis(ticked);
        if (ticked.isGameOver()) {
            persistGameStateSnapshot(ticked);
            soundEventLiveData.setValue(SoundEvent.GAME_END);
            onGameEnded();
        } else {
            long newRemaining = ticked.getClock().getRemainingMillis(activeSide);
            if (newRemaining > 0L && newRemaining <= LOW_TIME_THRESHOLD_MS) {
                int prevWholeSec = (int) Math.ceil(prevRemaining / 1000.0);
                int newWholeSec = (int) Math.ceil(newRemaining / 1000.0);
                if (newWholeSec < prevWholeSec) {
                    soundEventLiveData.setValue(SoundEvent.LOW_TIME_TICK);
                }
            }
        }
    }

    private void startClockLoop() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver() || clockRunning) {
            return;
        }
        clockRunning = true;
        lastTickElapsedRealtime = SystemClock.elapsedRealtime();
        mainHandler.removeCallbacks(clockTickRunnable);
        mainHandler.postDelayed(clockTickRunnable, CLOCK_UPDATE_INTERVAL_MS);
    }

    private void stopClockLoop() {
        clockRunning = false;
        lastTickElapsedRealtime = 0L;
        mainHandler.removeCallbacks(clockTickRunnable);
    }

    private void onGameEnded() {
        stopClockLoop();
        shutdownMoveSources();
        aiThinkingLiveData.setValue(false);
        selectedSquareLiveData.setValue(null);
        savedStateHandle.set(KEY_SELECTED_SQUARE, null);
        cancelPendingPromotion();
        pausedLiveData.setValue(false);
        savedStateHandle.set(KEY_PAUSED, false);
    }

    private void requestMoveFromActiveSource(@NonNull GameState state) {
        whiteMoveSource.cancel();
        blackMoveSource.cancel();

        if (state.isGameOver() || !state.hasLegalMoves()) {
            return;
        }

        MoveSource activeSource = getMoveSourceFor(state.getSideToMove());
        activeSource.requestMove(state, this::onMoveChosenBySource);
    }

    @NonNull
    private MoveSource getMoveSourceFor(@NonNull Color color) {
        return color == Color.WHITE ? whiteMoveSource : blackMoveSource;
    }

    private void shutdownMoveSources() {
        whiteMoveSource.cancel();
        blackMoveSource.cancel();
        if (whiteMoveSource instanceof AiMoveSource aiWhite) {
            aiWhite.shutdown();
        }
        if (blackMoveSource instanceof AiMoveSource aiBlack) {
            aiBlack.shutdown();
        }
    }

    @Override
    protected void onCleared() {
        stopClockLoop();
        mainHandler.removeCallbacksAndMessages(null);
        shutdownMoveSources();
        super.onCleared();
    }
}
