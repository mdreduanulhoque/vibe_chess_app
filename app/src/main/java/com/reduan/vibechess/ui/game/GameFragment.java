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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.ChessClock;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.GameState;
import com.reduan.vibechess.core.Move;
import com.reduan.vibechess.core.WinProbability;
import com.reduan.vibechess.ui.result.ResultFragment;

import java.util.List;

/**
 * Game screen hosting the custom {@link BoardView}, live chess clocks,
 * animated winning probability bar, captured-pieces trays, and pause overlay.
 * Portrait uses a stacked layout; landscape centers the board with side panels.
 */
public class GameFragment extends Fragment {

    public static final String ARG_INITIAL_CLOCK_MILLIS = "arg_initial_clock_millis";

    private GameViewModel viewModel;
    private boolean isUpdatingSwitch = false;
    private boolean hasRenderedProbabilityOnce = false;
    @Nullable
    private AlertDialog activePromotionDialog;

    @NonNull
    public static Bundle createArgs(long initialClockMillis) {
        Bundle args = new Bundle();
        args.putLong(ARG_INITIAL_CLOCK_MILLIS, initialClockMillis);
        return args;
    }

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

        long initialClockMillis = ChessClock.ONE_MINUTE_MILLIS;
        Bundle args = getArguments();
        if (args != null) {
            initialClockMillis = args.getLong(
                    ARG_INITIAL_CLOCK_MILLIS,
                    ChessClock.ONE_MINUTE_MILLIS
            );
        }

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        viewModel.initIfNeeded(initialClockMillis);
        hasRenderedProbabilityOnce = false;

        MaterialButton btnBack = view.findViewById(R.id.btn_game_back);
        MaterialButton btnPause = view.findViewById(R.id.btn_game_pause);
        MaterialButton btnResume = view.findViewById(R.id.btn_game_resume);
        TextView tvTurnIndicator = view.findViewById(R.id.tv_turn_indicator);
        SwitchMaterial switchRotateBlack = view.findViewById(R.id.switch_rotate_black);
        BoardView boardView = view.findViewById(R.id.board_view);
        View pauseOverlay = view.findViewById(R.id.pause_overlay);

        MaterialCardView cardBlack = view.findViewById(R.id.card_player_black);
        MaterialCardView cardWhite = view.findViewById(R.id.card_player_white);
        MaterialCardView cardClockBlack = view.findViewById(R.id.card_clock_black);
        MaterialCardView cardClockWhite = view.findViewById(R.id.card_clock_white);
        TextView tvClockBlack = view.findViewById(R.id.tv_clock_black);
        TextView tvClockWhite = view.findViewById(R.id.tv_clock_white);

        CapturedPiecesTrayView trayCapturedBlack = view.findViewById(R.id.tray_captured_black);
        CapturedPiecesTrayView trayCapturedWhite = view.findViewById(R.id.tray_captured_white);

        TextView tvProbWhite = view.findViewById(R.id.tv_prob_white);
        TextView tvProbBlack = view.findViewById(R.id.tv_prob_black);
        WinProbabilityBarView winProbabilityBar = view.findViewById(R.id.win_probability_bar);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        btnPause.setOnClickListener(v -> viewModel.pauseGame());
        btnResume.setOnClickListener(v -> viewModel.resumeGame());

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
            cardClockBlack.setRotation(rotate ? 180f : 0f);
            trayCapturedBlack.setRotateIcons180(rotate);

            isUpdatingSwitch = true;
            if (switchRotateBlack.isChecked() != rotate) {
                switchRotateBlack.setChecked(rotate);
            }
            isUpdatingSwitch = false;
        });

        viewModel.getPaused().observe(getViewLifecycleOwner(), paused -> {
            boolean isPaused = Boolean.TRUE.equals(paused);
            pauseOverlay.setVisibility(isPaused ? View.VISIBLE : View.GONE);
            btnPause.setEnabled(!isPaused);
            boardView.setInteractionEnabled(viewModel.isCurrentTurnHumanInteractive());
            if (isPaused) {
                dismissPromotionDialog();
            } else {
                List<Move> pendingPromotions = viewModel.getPendingPromotionMoves().getValue();
                GameState current = viewModel.getGameState().getValue();
                if (pendingPromotions != null && !pendingPromotions.isEmpty() && current != null) {
                    showPromotionDialog(current.getSideToMove(), pendingPromotions);
                }
            }
        });

        viewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            boardView.setGameState(state);
            boardView.setInteractionEnabled(viewModel.isCurrentTurnHumanInteractive());

            updateTurnAndClocksUi(
                    state,
                    tvTurnIndicator,
                    cardWhite,
                    cardBlack,
                    cardClockWhite,
                    cardClockBlack,
                    tvClockWhite,
                    tvClockBlack
            );
            updateWinProbabilityUi(state, tvProbWhite, tvProbBlack, winProbabilityBar);
            updateCapturedTraysUi(state, trayCapturedWhite, trayCapturedBlack);

            if (state.isGameOver() && viewModel.markResultNavigated()) {
                dismissPromotionDialog();
                Bundle resultArgs = ResultFragment.createArgs(
                        state.getResult(),
                        state.points(Color.WHITE),
                        state.points(Color.BLACK),
                        state.getClock().getInitialMillis()
                );
                Navigation.findNavController(view).navigate(R.id.action_game_to_result, resultArgs);
            }
        });

        viewModel.getSelectedSquare().observe(getViewLifecycleOwner(), boardView::setSelectedSquare);

        viewModel.getPendingPromotionMoves().observe(getViewLifecycleOwner(), promotionMoves -> {
            dismissPromotionDialog();
            GameState state = viewModel.getGameState().getValue();
            if (!viewModel.isPaused()
                    && promotionMoves != null
                    && !promotionMoves.isEmpty()
                    && state != null
                    && !state.isGameOver()) {
                showPromotionDialog(state.getSideToMove(), promotionMoves);
            }
        });

        viewModel.getSkippedTurnNotice().observe(getViewLifecycleOwner(), skippedSide -> {
            if (skippedSide == null) {
                return;
            }
            int msgRes = skippedSide == Color.WHITE
                    ? R.string.game_turn_skipped_white
                    : R.string.game_turn_skipped_black;
            Snackbar.make(view, msgRes, Snackbar.LENGTH_SHORT).show();
            viewModel.consumeSkippedTurnNotice();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        // Auto-pause when the app goes to background, but NOT on device rotation
        if (viewModel != null && !requireActivity().isChangingConfigurations()) {
            viewModel.pauseGame();
        }
    }

    private void updateTurnAndClocksUi(@NonNull GameState state,
                                       @NonNull TextView tvTurnIndicator,
                                       @NonNull MaterialCardView cardWhite,
                                       @NonNull MaterialCardView cardBlack,
                                       @NonNull MaterialCardView cardClockWhite,
                                       @NonNull MaterialCardView cardClockBlack,
                                       @NonNull TextView tvClockWhite,
                                       @NonNull TextView tvClockBlack) {
        boolean isWhiteTurn = state.getSideToMove() == Color.WHITE;
        tvTurnIndicator.setText(isWhiteTurn ? R.string.game_turn_white : R.string.game_turn_black);

        ChessClock clock = state.getClock();
        tvClockWhite.setText(ClockFormatter.formatMillis(clock.getWhiteRemainingMillis()));
        tvClockBlack.setText(ClockFormatter.formatMillis(clock.getBlackRemainingMillis()));

        boolean isWhiteClockActive = clock.getActiveSide() == Color.WHITE && !state.isGameOver();
        boolean isBlackClockActive = clock.getActiveSide() == Color.BLACK && !state.isGameOver();

        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.onPrimary);
        int bgColor = ContextCompat.getColor(requireContext(), R.color.background);
        int textColor = ContextCompat.getColor(requireContext(), R.color.text);
        int subtleStroke = ContextCompat.getColor(requireContext(), R.color.stroke_subtle);

        cardWhite.setStrokeColor(isWhiteTurn ? primaryColor : subtleStroke);
        cardBlack.setStrokeColor(isWhiteTurn ? subtleStroke : primaryColor);

        // Highlight the active side's clock in orange
        cardClockWhite.setCardBackgroundColor(isWhiteClockActive ? primaryColor : bgColor);
        cardClockWhite.setStrokeColor(isWhiteClockActive ? primaryColor : subtleStroke);
        tvClockWhite.setTextColor(isWhiteClockActive ? onPrimaryColor : textColor);

        cardClockBlack.setCardBackgroundColor(isBlackClockActive ? primaryColor : bgColor);
        cardClockBlack.setStrokeColor(isBlackClockActive ? primaryColor : subtleStroke);
        tvClockBlack.setTextColor(isBlackClockActive ? onPrimaryColor : textColor);
    }

    private void updateWinProbabilityUi(@NonNull GameState state,
                                        @NonNull TextView tvProbWhite,
                                        @NonNull TextView tvProbBlack,
                                        @NonNull WinProbabilityBarView winProbabilityBar) {
        WinProbability prob = WinProbability.compute(state);
        tvProbWhite.setText(getString(R.string.game_prob_white_format, prob.getWhitePercent()));
        tvProbBlack.setText(getString(R.string.game_prob_black_format, prob.getBlackPercent()));
        winProbabilityBar.setWhitePercent(prob.getWhitePercent(), hasRenderedProbabilityOnce);
        hasRenderedProbabilityOnce = true;
    }

    private void updateCapturedTraysUi(@NonNull GameState state,
                                       @NonNull CapturedPiecesTrayView trayCapturedWhite,
                                       @NonNull CapturedPiecesTrayView trayCapturedBlack) {
        // White's tray displays the Black pieces captured by White; Black's tray displays White pieces captured by Black
        trayCapturedWhite.setCapturedPieces(state.getCapturedPiecesOfColor(Color.BLACK));
        trayCapturedBlack.setCapturedPieces(state.getCapturedPiecesOfColor(Color.WHITE));
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
