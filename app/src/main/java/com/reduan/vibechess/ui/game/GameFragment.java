package com.reduan.vibechess.ui.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.GameState;
import com.reduan.vibechess.core.Move;

import java.util.List;

/**
 * Game screen hosting the custom {@link BoardView} and placeholders for clocks,
 * winning probability bar, and captured-pieces trays.
 * Portrait uses a stacked layout; landscape centers the board with side panels.
 */
public class GameFragment extends Fragment {

    private GameViewModel viewModel;
    private boolean isUpdatingSwitch = false;
    @Nullable
    private AlertDialog activePromotionDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        MaterialButton btnBack = view.findViewById(R.id.btn_game_back);
        TextView tvTurnIndicator = view.findViewById(R.id.tv_turn_indicator);
        SwitchMaterial switchRotateBlack = view.findViewById(R.id.switch_rotate_black);
        BoardView boardView = view.findViewById(R.id.board_view);
        MaterialCardView cardBlack = view.findViewById(R.id.card_player_black);
        MaterialCardView cardWhite = view.findViewById(R.id.card_player_white);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        boardView.setOnSquareSelectedListener(square -> viewModel.setSelectedSquare(square));
        boardView.setOnMoveSelectedListener(move -> viewModel.onHumanMoveSelected(move));
        boardView.setOnPromotionRequestedListener(
                (sideToMove, promotionMoves) -> viewModel.requestPromotionChoice(promotionMoves)
        );

        switchRotateBlack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitch) {
                return;
            }
            viewModel.setRotateBlackPieces(isChecked);
        });

        viewModel.getRotateBlackPieces().observe(getViewLifecycleOwner(), rotate -> {
            if (rotate == null) {
                return;
            }
            boardView.setFlipped(rotate);
            isUpdatingSwitch = true;
            if (switchRotateBlack.isChecked() != rotate) {
                switchRotateBlack.setChecked(rotate);
            }
            isUpdatingSwitch = false;
        });

        viewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            boardView.setGameState(state);
            boardView.setInteractionEnabled(viewModel.isCurrentTurnHumanInteractive());
            updateTurnUi(state, tvTurnIndicator, cardWhite, cardBlack);
        });

        viewModel.getSelectedSquare().observe(getViewLifecycleOwner(), boardView::setSelectedSquare);

        viewModel.getPendingPromotionMoves().observe(getViewLifecycleOwner(), promotionMoves -> {
            dismissPromotionDialog();
            GameState state = viewModel.getGameState().getValue();
            if (promotionMoves != null && !promotionMoves.isEmpty() && state != null) {
                showPromotionDialog(state.getSideToMove(), promotionMoves);
            }
        });
    }

    private void updateTurnUi(@NonNull GameState state,
                              @NonNull TextView tvTurnIndicator,
                              @NonNull MaterialCardView cardWhite,
                              @NonNull MaterialCardView cardBlack) {
        boolean isWhiteTurn = state.getSideToMove() == Color.WHITE;
        tvTurnIndicator.setText(isWhiteTurn ? R.string.game_turn_white : R.string.game_turn_black);

        int activeStroke = ContextCompat.getColor(requireContext(), R.color.primary);
        int idleStroke = ContextCompat.getColor(requireContext(), R.color.stroke_subtle);
        cardWhite.setStrokeColor(isWhiteTurn ? activeStroke : idleStroke);
        cardBlack.setStrokeColor(isWhiteTurn ? idleStroke : activeStroke);
    }

    private void showPromotionDialog(@NonNull Color sideToMove,
                                     @NonNull List<Move> promotionMoves) {
        activePromotionDialog = PromotionDialogHelper.show(
                requireContext(),
                sideToMove,
                promotionMoves,
                new PromotionDialogHelper.PromotionSelectionCallback() {
                    @Override
                    public void onPromotionMoveSelected(@NonNull Move chosenMove) {
                        activePromotionDialog = null;
                        viewModel.onHumanMoveSelected(chosenMove);
                    }

                    @Override
                    public void onPromotionCancelled() {
                        activePromotionDialog = null;
                        viewModel.cancelPendingPromotion();
                    }
                }
        );
    }

    private void dismissPromotionDialog() {
        if (activePromotionDialog != null) {
            if (activePromotionDialog.isShowing()) {
                activePromotionDialog.dismiss();
            }
            activePromotionDialog = null;
        }
    }

    @Override
    public void onDestroyView() {
        dismissPromotionDialog();
        super.onDestroyView();
    }
}
