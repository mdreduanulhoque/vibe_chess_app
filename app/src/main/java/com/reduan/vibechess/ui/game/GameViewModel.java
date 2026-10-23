package com.reduan.vibechess.ui.game;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
 * ViewModel holding the active {@link GameState} from {@code :game-core} and coordinating turns
 * between White and Black {@link MoveSource} instances. Survives configuration changes (rotation).
 */
public class GameViewModel extends AndroidViewModel {

    private final SettingsPreferences settingsPreferences;
    private final MoveSource whiteMoveSource;
    private final MoveSource blackMoveSource;

    private final MutableLiveData<GameState> gameStateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Square> selectedSquareLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<List<Move>> pendingPromotionMovesLiveData = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> rotateBlackPiecesLiveData = new MutableLiveData<>();

    public GameViewModel(@NonNull Application application) {
        super(application);
        this.settingsPreferences = new SettingsPreferences(application);
        this.whiteMoveSource = new LocalHumanMoveSource();
        this.blackMoveSource = new LocalHumanMoveSource();

        this.rotateBlackPiecesLiveData.setValue(
                settingsPreferences.isRotateOpponentPiecesEnabled()
        );

        GameState initialState = GameState.initial();
        this.gameStateLiveData.setValue(initialState);
        requestMoveFromActiveSource(initialState);
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
        if (!Objects.equals(selectedSquareLiveData.getValue(), square)) {
            selectedSquareLiveData.setValue(square);
        }
    }

    @NonNull
    public LiveData<List<Move>> getPendingPromotionMoves() {
        return pendingPromotionMovesLiveData;
    }

    public void requestPromotionChoice(@NonNull List<Move> promotionMoves) {
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

    /**
     * Returns true if the current side to move is controlled by a local human on this device.
     */
    public boolean isCurrentTurnHumanInteractive() {
        GameState current = gameStateLiveData.getValue();
        if (current == null || current.isGameOver()) {
            return false;
        }
        return getMoveSourceFor(current.getSideToMove()).isHumanInteractive();
    }

    /**
     * Submits a move selected by the local human player on the board UI.
     */
    public void onHumanMoveSelected(@NonNull Move move) {
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

        // Per PROJECT_SPEC.md: if a side has no legal move, its turn is skipped.
        if (!nextState.isGameOver() && !nextState.hasLegalMoves()) {
            nextState = nextState.skipTurn();
        }

        gameStateLiveData.setValue(nextState);
        requestMoveFromActiveSource(nextState);
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
        whiteMoveSource.cancel();
        blackMoveSource.cancel();
        super.onCleared();
    }
}
