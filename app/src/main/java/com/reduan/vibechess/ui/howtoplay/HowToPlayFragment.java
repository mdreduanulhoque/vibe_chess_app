package com.reduan.vibechess.ui.howtoplay;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.PieceType;
import com.reduan.vibechess.data.SettingsPreferences;
import com.reduan.vibechess.ui.game.BoardDemoConfig;
import com.reduan.vibechess.ui.game.BoardView;
import com.reduan.vibechess.ui.game.PieceDrawableMapper;
import com.reduan.vibechess.ui.game.WinProbabilityBarView;

import java.util.List;

/**
 * Bilingual (English / Bangla) visual "How to Play" screen that explains all 18 rules
 * in order using code-drawn read-only {@link BoardView} mini-boards, a visual points table,
 * timer badges, and a live {@link WinProbabilityBarView}.
 */
public class HowToPlayFragment extends Fragment {

    public static final String ARG_FIRST_LAUNCH = "arg_first_launch";
    private static final int SAMPLE_WHITE_PROBABILITY = 72;

    /**
     * Creates navigation arguments for {@link HowToPlayFragment}.
     *
     * @param isFirstLaunch true when launched automatically on first app start
     */
    @NonNull
    public static Bundle createArgs(boolean isFirstLaunch) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_FIRST_LAUNCH, isFirstLaunch);
        return args;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_how_to_play, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = requireContext();
        SettingsPreferences prefs = new SettingsPreferences(context);
        prefs.setHasSeenHowToPlay(true);

        Bundle args = getArguments();
        boolean isFirstLaunch = args != null && args.getBoolean(ARG_FIRST_LAUNCH, false);

        MaterialButton btnBack = view.findViewById(R.id.btn_how_to_play_back);
        MaterialButton btnSkip = view.findViewById(R.id.btn_how_to_play_skip);
        MaterialButton btnGotIt = view.findViewById(R.id.btn_how_to_play_got_it);
        LinearLayout container = view.findViewById(R.id.how_to_play_sections_container);

        if (isFirstLaunch) {
            btnBack.setVisibility(View.GONE);
            btnSkip.setVisibility(View.VISIBLE);
        } else {
            btnBack.setVisibility(View.VISIBLE);
            btnSkip.setVisibility(View.GONE);
        }

        View.OnClickListener finishAction = v -> navigateOut(v, isFirstLaunch);
        btnBack.setOnClickListener(finishAction);
        btnSkip.setOnClickListener(finishAction);
        btnGotIt.setOnClickListener(finishAction);

        if (isFirstLaunch) {
            requireActivity().getOnBackPressedDispatcher().addCallback(
                    getViewLifecycleOwner(),
                    new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            navigateOut(view, true);
                        }
                    }
            );
        }

        populateSections(context, container);
    }

    private void navigateOut(@NonNull View view, boolean isFirstLaunch) {
        if (!isAdded()) {
            return;
        }
        new SettingsPreferences(requireContext()).setHasSeenHowToPlay(true);
        NavController navController = Navigation.findNavController(view);
        if (isFirstLaunch) {
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.howToPlayFragment) {
                navController.navigate(R.id.action_how_to_play_to_home);
            }
        } else {
            navController.navigateUp();
        }
    }

    private void populateSections(@NonNull Context context, @NonNull LinearLayout container) {
        container.removeAllViews();
        List<HowToPlayRulesProvider.RuleSection> sections = HowToPlayRulesProvider.getSections();
        for (int i = 0; i < sections.size(); i++) {
            HowToPlayRulesProvider.RuleSection section = sections.get(i);
            View card = createSectionCard(context, section, i == 0);
            container.addView(card);
        }
    }

    @NonNull
    private View createSectionCard(@NonNull Context context,
                                   @NonNull HowToPlayRulesProvider.RuleSection section,
                                   boolean isFirst) {
        int spacingMd = dimenPx(context, R.dimen.spacing_md);
        int spacingSm = dimenPx(context, R.dimen.spacing_sm);
        int spacingXs = dimenPx(context, R.dimen.spacing_xs);

        MaterialCardView card = new MaterialCardView(context);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = isFirst ? spacingSm : spacingMd;
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface));
        card.setRadius(context.getResources().getDimension(R.dimen.corner_radius_lg));
        card.setCardElevation(0f);
        card.setStrokeWidth(dimenPx(context, R.dimen.stroke_width));
        card.setStrokeColor(ContextCompat.getColor(
                context,
                section.highlightCardBorder ? R.color.primary : R.color.stroke_subtle
        ));

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(spacingMd, spacingMd, spacingMd, spacingMd);
        card.addView(inner, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Section Title
        TextView titleView = new TextView(context);
        titleView.setText(section.titleRes);
        titleView.setTextColor(ContextCompat.getColor(
                context,
                section.highlightCardBorder ? R.color.primary : R.color.text
        ));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_section_label));
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        inner.addView(titleView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 2. Mini-board illustration (single or 2-frame before/after)
        if (section.extraVisualType == HowToPlayRulesProvider.ExtraVisualType.DUAL_BOARD_EN_PASSANT
                && section.secondaryBoard != null) {
            View dualView = createDualBoardIllustration(
                    context, section.primaryBoard, section.secondaryBoard
            );
            LinearLayout.LayoutParams dualParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dualParams.topMargin = spacingSm;
            inner.addView(dualView, dualParams);
        } else {
            int boardSize = dimenPx(context, R.dimen.demo_board_size);
            BoardView boardView = new BoardView(context);
            boardView.setDemoConfig(section.primaryBoard);
            LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(boardSize, boardSize);
            boardParams.gravity = Gravity.CENTER_HORIZONTAL;
            boardParams.topMargin = spacingSm;
            inner.addView(boardView, boardParams);
        }

        // 3. Optional extra visual widget (Points Table, Timer Badges, or Win Probability Bar)
        if (section.extraVisualType == HowToPlayRulesProvider.ExtraVisualType.POINTS_TABLE) {
            View table = createPointsTableView(context);
            LinearLayout.LayoutParams tableParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            tableParams.topMargin = spacingSm;
            inner.addView(table, tableParams);
        } else if (section.extraVisualType == HowToPlayRulesProvider.ExtraVisualType.TIMER_BADGES) {
            View timers = createTimerBadgesView(context);
            LinearLayout.LayoutParams timerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            timerParams.topMargin = spacingSm;
            inner.addView(timers, timerParams);
        } else if (section.extraVisualType == HowToPlayRulesProvider.ExtraVisualType.PROBABILITY_BAR) {
            View probView = createProbabilityBarSampleView(context);
            LinearLayout.LayoutParams probParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            probParams.topMargin = spacingSm;
            inner.addView(probView, probParams);
        }

        // 4. Short 1-2 line rule text (unclipped for both English and Bangla)
        TextView bodyView = new TextView(context);
        bodyView.setText(section.bodyRes);
        bodyView.setTextColor(ContextCompat.getColor(context, R.color.text));
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_home_subtitle));
        bodyView.setLineSpacing(0f, 1.15f);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyParams.topMargin = spacingXs;
        inner.addView(bodyView, bodyParams);

        return card;
    }

    @NonNull
    private View createDualBoardIllustration(@NonNull Context context,
                                             @NonNull BoardDemoConfig beforeConfig,
                                             @NonNull BoardDemoConfig afterConfig) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        int spacingSm = dimenPx(context, R.dimen.spacing_sm);
        View frameBefore = createLabeledMiniBoard(context, R.string.how_to_play_before, beforeConfig);
        View frameAfter = createLabeledMiniBoard(context, R.string.how_to_play_after, afterConfig);

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        leftParams.setMarginEnd(spacingSm / 2);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        rightParams.setMarginStart(spacingSm / 2);

        row.addView(frameBefore, leftParams);
        row.addView(frameAfter, rightParams);
        return row;
    }

    @NonNull
    private View createLabeledMiniBoard(@NonNull Context context,
                                        @StringRes int captionRes,
                                        @NonNull BoardDemoConfig config) {
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView label = new TextView(context);
        label.setText(captionRes);
        label.setTextColor(ContextCompat.getColor(context, R.color.primary));
        label.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_caption));
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        col.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        int boardSize = dimenPx(context, R.dimen.demo_board_dual_size);
        BoardView boardView = new BoardView(context);
        boardView.setDemoConfig(config);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(boardSize, boardSize);
        boardParams.topMargin = dimenPx(context, R.dimen.spacing_xxs);
        col.addView(boardView, boardParams);

        return col;
    }

    @NonNull
    private View createPointsTableView(@NonNull Context context) {
        LinearLayout table = new LinearLayout(context);
        table.setOrientation(LinearLayout.VERTICAL);

        PieceType[] types = {
                PieceType.PAWN,
                PieceType.KNIGHT,
                PieceType.BISHOP,
                PieceType.ROOK,
                PieceType.QUEEN,
                PieceType.KING
        };
        int[] nameResIds = {
                R.string.piece_pawn,
                R.string.piece_knight,
                R.string.piece_bishop,
                R.string.piece_rook,
                R.string.piece_queen,
                R.string.piece_king
        };

        int spacingXs = dimenPx(context, R.dimen.spacing_xs);
        for (int i = 0; i < types.length; i += 2) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (i > 0) {
                rowParams.topMargin = spacingXs;
            }

            View leftCell = createPointsTableCell(context, types[i], nameResIds[i]);
            View rightCell = createPointsTableCell(context, types[i + 1], nameResIds[i + 1]);

            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            );
            leftParams.setMarginEnd(spacingXs / 2);

            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            );
            rightParams.setMarginStart(spacingXs / 2);

            row.addView(leftCell, leftParams);
            row.addView(rightCell, rightParams);
            table.addView(row, rowParams);
        }

        return table;
    }

    @NonNull
    private View createPointsTableCell(@NonNull Context context,
                                       @NonNull PieceType type,
                                       @StringRes int pieceNameRes) {
        int spacingXs = dimenPx(context, R.dimen.spacing_xs);
        int iconSize = dimenPx(context, R.dimen.points_table_icon_size);

        MaterialCardView cellCard = new MaterialCardView(context);
        cellCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background));
        cellCard.setRadius(context.getResources().getDimension(R.dimen.corner_radius_sm));
        cellCard.setCardElevation(0f);
        cellCard.setStrokeWidth(dimenPx(context, R.dimen.stroke_width));
        cellCard.setStrokeColor(ContextCompat.getColor(
                context,
                type == PieceType.KING ? R.color.primary : R.color.stroke_subtle
        ));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(spacingXs, spacingXs, spacingXs, spacingXs);

        ImageView icon = new ImageView(context);
        icon.setImageResource(PieceDrawableMapper.getDrawableRes(Color.WHITE, type));
        row.addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView nameText = new TextView(context);
        nameText.setText(pieceNameRes);
        nameText.setTextColor(ContextCompat.getColor(context, R.color.text));
        nameText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_caption));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        nameParams.setMarginStart(spacingXs);
        row.addView(nameText, nameParams);

        int points = type.getPoints();
        TextView ptsText = new TextView(context);
        ptsText.setText(points == 1
                ? context.getString(R.string.how_to_play_points_value_single_format, points)
                : context.getString(R.string.how_to_play_points_value_format, points));
        ptsText.setTextColor(ContextCompat.getColor(context, R.color.primary));
        ptsText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_caption));
        ptsText.setTypeface(ptsText.getTypeface(), Typeface.BOLD);
        row.addView(ptsText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        cellCard.addView(row, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return cellCard;
    }

    @NonNull
    private View createTimerBadgesView(@NonNull Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        int spacingXs = dimenPx(context, R.dimen.spacing_xs);

        View activeBadge = createClockBadge(
                context,
                context.getString(R.string.game_turn_white),
                context.getString(R.string.how_to_play_clock_active_sample),
                true
        );
        View pausedBadge = createClockBadge(
                context,
                context.getString(R.string.game_player_black),
                context.getString(R.string.how_to_play_clock_paused_sample),
                false
        );

        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        leftParams.setMarginEnd(spacingXs);

        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        );
        rightParams.setMarginStart(spacingXs);

        row.addView(activeBadge, leftParams);
        row.addView(pausedBadge, rightParams);
        return row;
    }

    @NonNull
    private View createClockBadge(@NonNull Context context,
                                  @NonNull String label,
                                  @NonNull String timeText,
                                  boolean active) {
        int spacingXs = dimenPx(context, R.dimen.spacing_xs);
        MaterialCardView badge = new MaterialCardView(context);
        badge.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background));
        badge.setRadius(context.getResources().getDimension(R.dimen.corner_radius_md));
        badge.setCardElevation(0f);
        badge.setStrokeWidth(dimenPx(context, R.dimen.stroke_width));
        badge.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(
                context, active ? R.color.primary : R.color.stroke_subtle
        )));

        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(spacingXs, spacingXs, spacingXs, spacingXs);

        TextView labelTv = new TextView(context);
        labelTv.setText(label);
        labelTv.setTextColor(ContextCompat.getColor(
                context, active ? R.color.primary : R.color.text
        ));
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_caption));
        col.addView(labelTv);

        TextView clockTv = new TextView(context);
        clockTv.setText(timeText);
        clockTv.setTextColor(ContextCompat.getColor(
                context, active ? R.color.primary : R.color.text
        ));
        clockTv.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_section_label));
        clockTv.setTypeface(clockTv.getTypeface(), Typeface.BOLD);
        col.addView(clockTv);

        badge.addView(col);
        return badge;
    }

    @NonNull
    private View createProbabilityBarSampleView(@NonNull Context context) {
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);

        LinearLayout labelsRow = new LinearLayout(context);
        labelsRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView whiteLabel = new TextView(context);
        whiteLabel.setText(context.getString(
                R.string.game_prob_white_format, SAMPLE_WHITE_PROBABILITY
        ));
        whiteLabel.setTextColor(ContextCompat.getColor(context, R.color.primary));
        whiteLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_caption));
        whiteLabel.setTypeface(whiteLabel.getTypeface(), Typeface.BOLD);
        labelsRow.addView(whiteLabel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));

        TextView blackLabel = new TextView(context);
        blackLabel.setText(context.getString(
                R.string.game_prob_black_format, 100 - SAMPLE_WHITE_PROBABILITY
        ));
        blackLabel.setTextColor(ContextCompat.getColor(context, R.color.text));
        blackLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimension(R.dimen.text_caption));
        blackLabel.setTypeface(blackLabel.getTypeface(), Typeface.BOLD);
        labelsRow.addView(blackLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        wrapper.addView(labelsRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        WinProbabilityBarView barView = new WinProbabilityBarView(context);
        barView.setWhitePercent(SAMPLE_WHITE_PROBABILITY, false);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dimenPx(context, R.dimen.prob_bar_height)
        );
        barParams.topMargin = dimenPx(context, R.dimen.spacing_xxs);
        wrapper.addView(barView, barParams);

        return wrapper;
    }

    private static int dimenPx(@NonNull Context context, int dimenRes) {
        return context.getResources().getDimensionPixelSize(dimenRes);
    }
}
