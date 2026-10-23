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

import com.reduan.vibechess.core.ChessClock;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.GameState;
import com.reduan.vibechess.core.LocalHumanMoveSource;
import com.reduan.vibechess.core.Move;
import com.reduan.vibechess.core.MoveSource;
import com.reduan.vibechess.core.Square;
import com.reduan.vibechess.data.SettingsPreferences;

import java.util.List;
import java.util.Objects;

/**
 * ViewModel holding the active {@link GameState} from {@code :game-core}, driving the chess clock
 * via {@link SystemClock#elapsedRealtime()} deltas (~10x/sec on the main thread), managing pause/resume,
 * and coordinating turns between White and Black {@link MoveSource} instances.
 * Survives configuration changes (rotation) without clock drift.
 */
public class GameViewModel extends AndroidViewModel {

    private static final long CLOCK_UPDATE_INTERVAL_MS = 100L;

    private final SettingsPreferences settingsPreferences;
    private final MoveSource whiteMoveSource;
    private final MoveSource blackMoveSource;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<GameState> gameStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Square> selectedSquareLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<List<Move>> pendingPromotionMovesLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> rotateBlackPiecesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pausedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Color> skippedTurnNoticeLiveData = new MutableLiveData<>(null);

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
        super(application);
        this.settingsPreferences = new SettingsPreferences(application);
        this.whiteMoveSource = new LocalHumanMoveSource();
        this.blackMoveSource = new LocalHumanMoveSource();

        this.rotateBlackPiecesLiveData.setValue(
                settingsPreferences.isRotateOpponentPiecesEnabled()
        );
    }

    /**
     * Initializes the game with the given clock duration per side if not already initialized.
     * Safe to call from {@code Fragment.onViewCreated} across screen rotations.
     */
    public void initIfNeeded(long initialClockMillis) {
        if (initialized) {
            return;
        }
        initialized = true;
        long validMillis = initialClockMillis > 0 ? initialClockMillis : ChessClock.ONE_MINUTE_MILLIS;
        GameState initialState = GameState.initial(validMillis);
        gameStateLiveData.setValue(initialState);
        requestMoveFromActiveSource(initialState);
        startClockLoop();
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
        }
    }

    @NonNull
    public LiveData<List<Move>> getPendingPromotionMoves() {
        return pendingPromotionMovesLiveData;
    }

    public void requestPromotionChoice(@NonNull List<Move> promotionMoves) {
        if (Boolean.TRUE.equals(pausedLiveData.getValue())) {
            return;
        }
        pendingPromotionMovesLiveData.setValue(List.copyOf(promotionMoves));
    }

    public void cancelPendingPromotion() {
        pendingPromotionMovesLiveData.setValue(null);
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

    /**
     * Pauses the active game clock and hides the board behind the pause overlay.
     */
    public void pauseGame() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver() || isPaused()) {
            return;
        }
        applyElapsedRealtimeDelta();
        stopClockLoop();

        GameState afterTick = gameStateLiveData.getValue();
        if (afterTick != null && afterTick.isGameOver()) {
            return;
        }
        pausedLiveData.setValue(true);
    }

    /**
     * Resumes the game clock from pause.
     */
    public void resumeGame() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver() || !isPaused()) {
            return;
        }
        pausedLiveData.setValue(false);
        startClockLoop();
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
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver()) {
            return;
        }

        selectedSquareLiveData.setValue(null);
        pendingPromotionMovesLiveData.setValue(null);

        GameState nextState = current.applyMove(move);

        // Per PROJECT_SPEC.md: if a side has no legal move, its turn is skipped and its clock keeps running.
        if (!nextState.isGameOver() && !nextState.hasLegalMoves()) {
            Color skippedSide = nextState.getSideToMove();
            nextState = nextState.skipTurn();
            skippedTurnNoticeLiveData.setValue(skippedSide);
        }

        lastTickElapsedRealtime = SystemClock.elapsedRealtime();
        gameStateLiveData.setValue(nextState);

        if (nextState.isGameOver()) {
            onGameEnded();
        } else {
            requestMoveFromActiveSource(nextState);
        }
    }

    private void applyElapsedRealtimeDelta() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver()) {
            stopClockLoop();
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long delta = Math.max(0L, now - lastTickElapsedRealtime);
        lastTickElapsedRealtime = now;
        if (delta <= 0L) {
            return;
        }

        GameState ticked = current.tick(delta);
        gameStateLiveData.setValue(ticked);
        if (ticked.isGameOver()) {
            onGameEnded();
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
        mainHandler.removeCallbacks(clockTickRunnable);
    }

    private void onGameEnded() {
        stopClockLoop();
        whiteMoveSource.cancel();
        blackMoveSource.cancel();
        selectedSquareLiveData.setValue(null);
        pendingPromotionMovesLiveData.setValue(null);
        pausedLiveData.setValue(false);
    }

    private void requestMoveFromActiveSource(@NonNull GameState state) {
        whiteMoveSource.cancel();
        blackMoveSource.cancel();

        if (state.isGameOver()) {
            return;
        }

        MoveSource activeSource = getMoveSourceFor(state.getSideToMove());
        activeSource.requestMove(state, this::onMoveChosenBySource);
    }

    @NonNull
    private MoveSource getMoveSourceFor(@NonNull Color color) {
        return color == Color.WHITE ? whiteMoveSource : blackMoveSource;
    }

    @Override
    protected void onCleared() {
        stopClockLoop();
        whiteMoveSource.cancel();
        blackMoveSource.cancel();
        super.onCleared();
    }
}
