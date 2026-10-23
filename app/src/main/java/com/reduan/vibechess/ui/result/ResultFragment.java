package com.reduan.vibechess.ui.result;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.ChessClock;
import com.reduan.vibechess.core.GameResult;
import com.reduan.vibechess.ui.game.GameFragment;

import java.util.Locale;

/**
 * Result screen displayed when a player's clock reaches 0.
 * Shows the winner (or draw), reason ("Time ended, more points on board"),
 * final board points for both sides (shown ONLY here per PROJECT_SPEC.md),
 * and Rematch / Home buttons.
 */
public class ResultFragment extends Fragment {

    public static final String ARG_RESULT_NAME = "arg_result_name";
    public static final String ARG_WHITE_POINTS = "arg_white_points";
    public static final String ARG_BLACK_POINTS = "arg_black_points";
    public static final String ARG_INITIAL_CLOCK_MILLIS = "arg_initial_clock_millis";

    @NonNull
    public static Bundle createArgs(@NonNull GameResult result,
                                    int whitePoints,
                                    int blackPoints,
                                    long initialClockMillis) {
        Bundle args = new Bundle();
        args.putString(ARG_RESULT_NAME, result.name());
        args.putInt(ARG_WHITE_POINTS, whitePoints);
        args.putInt(ARG_BLACK_POINTS, blackPoints);
        args.putLong(ARG_INITIAL_CLOCK_MILLIS, initialClockMillis);
        return args;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        GameResult result = GameResult.DRAW;
        int whitePoints = 0;
        int blackPoints = 0;
        long initialClockMillis = ChessClock.ONE_MINUTE_MILLIS;

        if (args != null) {
            String resultName = args.getString(ARG_RESULT_NAME, GameResult.DRAW.name());
            try {
                result = GameResult.valueOf(resultName);
            } catch (IllegalArgumentException ignored) {
                result = GameResult.DRAW;
            }
            whitePoints = args.getInt(ARG_WHITE_POINTS, 0);
            blackPoints = args.getInt(ARG_BLACK_POINTS, 0);
            initialClockMillis = args.getLong(
                    ARG_INITIAL_CLOCK_MILLIS,
                    ChessClock.ONE_MINUTE_MILLIS
            );
        }

        TextView tvWinner = view.findViewById(R.id.tv_result_winner);
        TextView tvReason = view.findViewById(R.id.tv_result_reason);
        TextView tvWhitePoints = view.findViewById(R.id.tv_result_white_points);
        TextView tvBlackPoints = view.findViewById(R.id.tv_result_black_points);
        MaterialCardView cardScoreWhite = view.findViewById(R.id.card_score_white);
        MaterialCardView cardScoreBlack = view.findViewById(R.id.card_score_black);
        MaterialButton btnRematch = view.findViewById(R.id.btn_result_rematch);
        MaterialButton btnHome = view.findViewById(R.id.btn_result_home);

        int primaryStroke = ContextCompat.getColor(requireContext(), R.color.primary);
        int subtleStroke = ContextCompat.getColor(requireContext(), R.color.stroke_subtle);

        switch (result) {
            case WHITE_WINS -> {
                tvWinner.setText(R.string.result_white_wins);
                tvReason.setText(R.string.result_reason_win);
                cardScoreWhite.setStrokeColor(primaryStroke);
                cardScoreBlack.setStrokeColor(subtleStroke);
            }
            case BLACK_WINS -> {
                tvWinner.setText(R.string.result_black_wins);
                tvReason.setText(R.string.result_reason_win);
                cardScoreWhite.setStrokeColor(subtleStroke);
                cardScoreBlack.setStrokeColor(primaryStroke);
            }
            default -> {
                tvWinner.setText(R.string.result_draw);
                tvReason.setText(R.string.result_reason_draw);
                cardScoreWhite.setStrokeColor(primaryStroke);
                cardScoreBlack.setStrokeColor(primaryStroke);
            }
        }

        tvWhitePoints.setText(String.format(Locale.getDefault(), "%d", whitePoints));
        tvBlackPoints.setText(String.format(Locale.getDefault(), "%d", blackPoints));

        final long rematchClockMillis = initialClockMillis;
        btnRematch.setOnClickListener(v -> {
            Bundle gameArgs = GameFragment.createArgs(rematchClockMillis);
            Navigation.findNavController(v).navigate(R.id.action_result_to_game, gameArgs);
        });

        btnHome.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_result_to_home));
    }
}
