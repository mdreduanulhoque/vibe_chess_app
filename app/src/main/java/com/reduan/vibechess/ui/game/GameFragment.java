package com.reduan.vibechess.ui.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.AiDifficulty;
import com.reduan.vibechess.core.ChessClock;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.GameState;
import com.reduan.vibechess.core.Move;
import com.reduan.vibechess.core.WinProbability;
import com.reduan.vibechess.ui.result.ResultFragment;

import java.util.List;

/**
 * Game screen hosting the custom {@link BoardView}, live chess clocks,
 * animated winning probability bar, captured-pieces trays, subtle AI thinking indicator,
 * sound/haptic feedback, leave-game confirmation dialog, and pause overlay.
 * Supports both Single Player vs Computer and Multiplayer Pass-and-Play.
 * Portrait uses a stacked layout; landscape centers the board with side panels.
 */
public class GameFragment extends Fragment {

    public static final String ARG_INITIAL_CLOCK_MILLIS = "arg_initial_clock_millis";
    public static final String ARG_SINGLE_PLAYER = "arg_single_player";
    public static final String ARG_HUMAN_COLOR = "arg_human_color";
    public static final String ARG_AI_DIFFICULTY = "arg_ai_difficulty";
    public static final String ARG_RANDOM_COLOR_CHOICE = "arg_random_color_choice";

    private GameViewModel viewModel;
    @Nullable
    private GameSoundManager soundManager;
    private boolean isUpdatingSwitch = false;
    private boolean hasRenderedProbabilityOnce = false;
    @Nullable
    private AlertDialog activePromotionDialog;
    @Nullable
    private AlertDialog activeExitConfirmDialog;

    @NonNull
    public static Bundle createArgs(long initialClockMillis) {
        Bundle args = new Bundle();
        args.putLong(ARG_INITIAL_CLOCK_MILLIS, initialClockMillis);
        args.putBoolean(ARG_SINGLE_PLAYER, false);
        return args;
    }

    @NonNull
    public static Bundle createSinglePlayerArgs(long initialClockMillis,
                                                @NonNull Color humanColor,
                                                @NonNull AiDifficulty difficulty,
                                                boolean randomColorChoice) {
        Bundle args = new Bundle();
        args.putLong(ARG_INITIAL_CLOCK_MILLIS, initialClockMillis);
        args.putBoolean(ARG_SINGLE_PLAYER, true);
        args.putString(ARG_HUMAN_COLOR, humanColor.name());
        args.putString(ARG_AI_DIFFICULTY, difficulty.name());
        args.putBoolean(ARG_RANDOM_COLOR_CHOICE, randomColorChoice);
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

        soundManager = new GameSoundManager(requireContext());

        long initialClockMillis = ChessClock.ONE_MINUTE_MILLIS;
        boolean singlePlayer = false;
        Color humanColor = Color.WHITE;
        AiDifficulty difficulty = AiDifficulty.MEDIUM;
        boolean randomColorChoice = false;

        Bundle args = getArguments();
        if (args != null) {
            initialClockMillis = args.getLong(
                    ARG_INITIAL_CLOCK_MILLIS,
                    ChessClock.ONE_MINUTE_MILLIS
            );
            singlePlayer = args.getBoolean(ARG_SINGLE_PLAYER, false);
            randomColorChoice = args.getBoolean(ARG_RANDOM_COLOR_CHOICE, false);

            String colorName = args.getString(ARG_HUMAN_COLOR, Color.WHITE.name());
            try {
                humanColor = Color.valueOf(colorName);
            } catch (IllegalArgumentException ignored) {
                humanColor = Color.WHITE;
            }

            String diffName = args.getString(ARG_AI_DIFFICULTY, AiDifficulty.MEDIUM.name());
            try {
                difficulty = AiDifficulty.valueOf(diffName);
            } catch (IllegalArgumentException ignored) {
                difficulty = AiDifficulty.MEDIUM;
            }
        }

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        viewModel.initIfNeeded(
                initialClockMillis,
                singlePlayer,
                humanColor,
                difficulty,
                randomColorChoice
        );
        hasRenderedProbabilityOnce = false;

        MaterialButton btnBack = view.findViewById(R.id.btn_game_back);
        MaterialButton btnPause = view.findViewById(R.id.btn_game_pause);
        MaterialButton btnResume = view.findViewById(R.id.btn_game_resume);
        ProgressBar pbAiThinking = view.findViewById(R.id.pb_ai_thinking);
        TextView tvTurnIndicator = view.findViewById(R.id.tv_turn_indicator);
        SwitchMaterial switchRotateBlack = view.findViewById(R.id.switch_rotate_black);
        BoardView boardView = view.findViewById(R.id.board_view);
        View pauseOverlay = view.findViewById(R.id.pause_overlay);

        // Layout top and bottom player cards
        MaterialCardView cardTop = view.findViewById(R.id.card_player_black);
        MaterialCardView cardBottom = view.findViewById(R.id.card_player_white);
        TextView tvLabelTop = view.findViewById(R.id.tv_label_black);
        TextView tvLabelBottom = view.findViewById(R.id.tv_label_white);
        MaterialCardView cardClockTop = view.findViewById(R.id.card_clock_black);
        MaterialCardView cardClockBottom = view.findViewById(R.id.card_clock_white);
        TextView tvClockTop = view.findViewById(R.id.tv_clock_black);
        TextView tvClockBottom = view.findViewById(R.id.tv_clock_white);
        CapturedPiecesTrayView trayTop = view.findViewById(R.id.tray_captured_black);
        CapturedPiecesTrayView trayBottom = view.findViewById(R.id.tray_captured_white);

        // When playing Single Player as Black, flip the board so Black is at the bottom
        // and bind the bottom player panel to Black (You) and the top panel to White (AI).
        boolean flipPerspectiveForHumanBlack =
                viewModel.isSinglePlayer() && viewModel.getHumanColor() == Color.BLACK;
        boardView.setBoardPerspectiveFlipped(flipPerspectiveForHumanBlack);

        final MaterialCardView cardWhite = flipPerspectiveForHumanBlack ? cardTop : cardBottom;
        final MaterialCardView cardBlack = flipPerspectiveForHumanBlack ? cardBottom : cardTop;
        final MaterialCardView cardClockWhite = flipPerspectiveForHumanBlack ? cardClockTop : cardClockBottom;
        final MaterialCardView cardClockBlack = flipPerspectiveForHumanBlack ? cardClockBottom : cardClockTop;
        final TextView tvClockWhite = flipPerspectiveForHumanBlack ? tvClockTop : tvClockBottom;
        final TextView tvClockBlack = flipPerspectiveForHumanBlack ? tvClockBottom : tvClockTop;
        final CapturedPiecesTrayView trayCapturedWhite = flipPerspectiveForHumanBlack ? trayTop : trayBottom;
        final CapturedPiecesTrayView trayCapturedBlack = flipPerspectiveForHumanBlack ? trayBottom : trayTop;

        if (viewModel.isSinglePlayer()) {
            switchRotateBlack.setVisibility(View.GONE);
            boardView.setFlipped(false);
            cardClockTop.setRotation(0f);
            cardClockBottom.setRotation(0f);
            trayTop.setRotateIcons180(false);
            trayBottom.setRotateIcons180(false);

            if (flipPerspectiveForHumanBlack) {
                tvLabelTop.setText(R.string.game_player_white_ai);
                tvLabelBottom.setText(R.string.game_player_black_you);
            } else {
                tvLabelTop.setText(R.string.game_player_black_ai);
                tvLabelBottom.setText(R.string.game_player_white_you);
            }
        } else {
            switchRotateBlack.setVisibility(View.VISIBLE);
            tvLabelTop.setText(R.string.game_player_black);
            tvLabelBottom.setText(R.string.game_player_white);
        }

        TextView tvProbWhite = view.findViewById(R.id.tv_prob_white);
        TextView tvProbBlack = view.findViewById(R.id.tv_prob_black);
        WinProbabilityBarView winProbabilityBar = view.findViewById(R.id.win_probability_bar);

        btnBack.setOnClickListener(v -> handleBackNavigationRequest(view));
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBackNavigationRequest(view);
                    }
                }
        );

        btnPause.setOnClickListener(v -> viewModel.pauseGame());
        btnResume.setOnClickListener(v -> viewModel.resumeGame());

        boardView.setOnSquareSelectedListener(square -> viewModel.setSelectedSquare(square));
        boardView.setOnMoveSelectedListener(move -> viewModel.onHumanMoveSelected(move));
        boardView.setOnPromotionRequestedListener(
                (sideToMove, promotionMoves) -> viewModel.requestPromotionChoice(promotionMoves)
        );

        switchRotateBlack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingSwitch || viewModel.isSinglePlayer()) {
                return;
            }
            viewModel.setRotateBlackPieces(isChecked);
        });

        viewModel.getRotateBlackPieces().observe(getViewLifecycleOwner(), rotate -> {
            if (rotate == null || viewModel.isSinglePlayer()) {
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

        viewModel.getSoundEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null || soundManager == null) {
                return;
            }
            switch (event) {
                case MOVE -> soundManager.playMove();
                case CAPTURE -> soundManager.playCaptureAndHaptics(boardView);
                case LOW_TIME_TICK -> soundManager.playLowTimeTick();
                case GAME_END -> soundManager.playGameEnd();
            }
            viewModel.consumeSoundEvent();
        });

        viewModel.getAiThinking().observe(getViewLifecycleOwner(), thinking -> {
            boolean showThinking = Boolean.TRUE.equals(thinking) && !viewModel.isPaused();
            pbAiThinking.setVisibility(showThinking ? View.VISIBLE : View.GONE);
            GameState current = viewModel.getGameState().getValue();
            if (current != null) {
                updateTurnIndicatorText(current, showThinking, tvTurnIndicator);
            }
        });

        viewModel.getPaused().observe(getViewLifecycleOwner(), paused -> {
            boolean isPaused = Boolean.TRUE.equals(paused);
            pauseOverlay.setVisibility(isPaused ? View.VISIBLE : View.GONE);
            btnPause.setEnabled(!isPaused);
            boardView.setInteractionEnabled(viewModel.isCurrentTurnHumanInteractive());
            if (isPaused) {
                pbAiThinking.setVisibility(View.GONE);
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

            boolean isAiThinking = Boolean.TRUE.equals(viewModel.getAiThinking().getValue())
                    && !viewModel.isPaused();
            updateTurnAndClocksUi(
                    state,
                    isAiThinking,
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
                dismissExitConfirmDialog();
                Bundle resultArgs = ResultFragment.createArgs(
                        state.getResult(),
                        state.points(Color.WHITE),
                        state.points(Color.BLACK),
                        state.getClock().getInitialMillis(),
                        viewModel.isSinglePlayer(),
                        viewModel.getHumanColor(),
                        viewModel.getAiDifficulty(),
                        viewModel.isRandomColorChoice()
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

    private void handleBackNavigationRequest(@NonNull View rootView) {
        GameState current = viewModel != null ? viewModel.getGameState().getValue() : null;
        if (current == null || current.isGameOver()) {
            if (viewModel != null) {
                viewModel.onExitGame();
            }
            Navigation.findNavController(rootView).navigateUp();
            return;
        }
        showExitConfirmationDialog(rootView);
    }

    private void showExitConfirmationDialog(@NonNull View rootView) {
        if (activeExitConfirmDialog != null && activeExitConfirmDialog.isShowing()) {
            return;
        }
        activeExitConfirmDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.game_exit_confirm_title)
                .setMessage(R.string.game_exit_confirm_message)
                .setNegativeButton(R.string.game_exit_confirm_stay, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.game_exit_confirm_leave, (dialog, which) -> {
                    dialog.dismiss();
                    if (viewModel != null) {
                        viewModel.onExitGame();
                    }
                    Navigation.findNavController(rootView).navigateUp();
                })
                .setOnDismissListener(dialog -> activeExitConfirmDialog = null)
                .show();
    }

    private void dismissExitConfirmDialog() {
        if (activeExitConfirmDialog != null) {
            if (activeExitConfirmDialog.isShowing()) {
                activeExitConfirmDialog.dismiss();
            }
            activeExitConfirmDialog = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Auto-pause when the app goes to background, but NOT on device rotation
        if (viewModel != null && !requireActivity().isChangingConfigurations()) {
            viewModel.pauseGame();
        }
    }

    private void updateTurnIndicatorText(@NonNull GameState state,
                                         boolean isAiThinking,
                                         @NonNull TextView tvTurnIndicator) {
        if (isAiThinking && !state.isGameOver()) {
            tvTurnIndicator.setText(R.string.game_ai_thinking);
        } else {
            boolean isWhiteTurn = state.getSideToMove() == Color.WHITE;
            tvTurnIndicator.setText(isWhiteTurn ? R.string.game_turn_white : R.string.game_turn_black);
        }
    }

    private void updateTurnAndClocksUi(@NonNull GameState state,
                                       boolean isAiThinking,
                                       @NonNull TextView tvTurnIndicator,
                                       @NonNull MaterialCardView cardWhite,
                                       @NonNull MaterialCardView cardBlack,
                                       @NonNull MaterialCardView cardClockWhite,
                                       @NonNull MaterialCardView cardClockBlack,
                                       @NonNull TextView tvClockWhite,
                                       @NonNull TextView tvClockBlack) {
        updateTurnIndicatorText(state, isAiThinking, tvTurnIndicator);

        boolean isWhiteTurn = state.getSideToMove() == Color.WHITE;
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
        dismissExitConfirmDialog();
        if (soundManager != null) {
            soundManager.release();
            soundManager = null;
        }
        super.onDestroyView();
    }
}
