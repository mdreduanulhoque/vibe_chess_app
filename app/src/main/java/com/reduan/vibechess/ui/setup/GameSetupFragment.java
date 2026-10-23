package com.reduan.vibechess.ui.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.AiDifficulty;
import com.reduan.vibechess.core.ChessClock;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.data.SettingsPreferences;
import com.reduan.vibechess.ui.game.GameFragment;

import java.util.Random;

/**
 * Game Setup screen supporting both:
 * <ul>
 *   <li>Single Player vs Computer: player color (White / Black / Random),
 *       AI difficulty (Easy / Medium / Hard), and time per side (1 or 2 minutes).</li>
 *   <li>Multiplayer (Pass-and-Play): time per side (1 or 2 minutes) and
 *       "Rotate opponent's pieces 180°" toggle.</li>
 * </ul>
 */
public class GameSetupFragment extends Fragment {

    public static final String ARG_SINGLE_PLAYER = "arg_single_player";

    public static final String COLOR_CHOICE_WHITE = "WHITE";
    public static final String COLOR_CHOICE_BLACK = "BLACK";
    public static final String COLOR_CHOICE_RANDOM = "RANDOM";

    private static final String STATE_SELECTED_TIME_MILLIS = "state_selected_time_millis";
    private static final String STATE_SELECTED_COLOR_CHOICE = "state_selected_color_choice";
    private static final String STATE_SELECTED_DIFFICULTY = "state_selected_difficulty";

    private boolean singlePlayerMode = false;
    private long selectedClockMillis = ChessClock.ONE_MINUTE_MILLIS;
    private String selectedColorChoice = COLOR_CHOICE_WHITE;
    private AiDifficulty selectedDifficulty = AiDifficulty.MEDIUM;

    @NonNull
    public static Bundle createArgs(boolean singlePlayer) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_SINGLE_PLAYER, singlePlayer);
        return args;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_game_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            singlePlayerMode = args.getBoolean(ARG_SINGLE_PLAYER, false);
        }

        if (savedInstanceState != null) {
            selectedClockMillis = savedInstanceState.getLong(
                    STATE_SELECTED_TIME_MILLIS,
                    ChessClock.ONE_MINUTE_MILLIS
            );
            selectedColorChoice = savedInstanceState.getString(
                    STATE_SELECTED_COLOR_CHOICE,
                    COLOR_CHOICE_WHITE
            );
            String diffName = savedInstanceState.getString(
                    STATE_SELECTED_DIFFICULTY,
                    AiDifficulty.MEDIUM.name()
            );
            try {
                selectedDifficulty = AiDifficulty.valueOf(diffName);
            } catch (IllegalArgumentException ignored) {
                selectedDifficulty = AiDifficulty.MEDIUM;
            }
        }

        SettingsPreferences prefs = new SettingsPreferences(requireContext());

        MaterialButton btnBack = view.findViewById(R.id.btn_setup_back);
        TextView tvSubtitle = view.findViewById(R.id.tv_setup_subtitle);
        MaterialCardView cardColor = view.findViewById(R.id.card_setup_color);
        MaterialCardView cardDifficulty = view.findViewById(R.id.card_setup_difficulty);
        MaterialCardView cardRotateOpponent = view.findViewById(R.id.card_setup_rotate_opponent);

        RadioGroup rgColorChoice = view.findViewById(R.id.rg_color_choice);
        RadioGroup rgDifficultyChoice = view.findViewById(R.id.rg_difficulty_choice);
        RadioGroup rgTimeChoice = view.findViewById(R.id.rg_time_choice);
        SwitchMaterial switchRotate = view.findViewById(R.id.switch_setup_rotate_opponent);
        MaterialButton btnStart = view.findViewById(R.id.btn_start_game);

        btnBack.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }
            var navController = Navigation.findNavController(v);
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.gameSetupFragment) {
                navController.navigateUp();
            }
        });

        if (singlePlayerMode) {
            tvSubtitle.setText(R.string.setup_subtitle_single_player);
            cardColor.setVisibility(View.VISIBLE);
            cardDifficulty.setVisibility(View.VISIBLE);
            cardRotateOpponent.setVisibility(View.GONE);
        } else {
            tvSubtitle.setText(R.string.setup_subtitle_multiplayer);
            cardColor.setVisibility(View.GONE);
            cardDifficulty.setVisibility(View.GONE);
            cardRotateOpponent.setVisibility(View.VISIBLE);
        }

        // Restore color selection
        switch (selectedColorChoice) {
            case COLOR_CHOICE_BLACK -> rgColorChoice.check(R.id.rb_color_black);
            case COLOR_CHOICE_RANDOM -> rgColorChoice.check(R.id.rb_color_random);
            default -> rgColorChoice.check(R.id.rb_color_white);
        }
        rgColorChoice.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_color_black) {
                selectedColorChoice = COLOR_CHOICE_BLACK;
            } else if (checkedId == R.id.rb_color_random) {
                selectedColorChoice = COLOR_CHOICE_RANDOM;
            } else {
                selectedColorChoice = COLOR_CHOICE_WHITE;
            }
        });

        // Restore difficulty selection
        switch (selectedDifficulty) {
            case EASY -> rgDifficultyChoice.check(R.id.rb_difficulty_easy);
            case HARD -> rgDifficultyChoice.check(R.id.rb_difficulty_hard);
            default -> rgDifficultyChoice.check(R.id.rb_difficulty_medium);
        }
        rgDifficultyChoice.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_difficulty_easy) {
                selectedDifficulty = AiDifficulty.EASY;
            } else if (checkedId == R.id.rb_difficulty_hard) {
                selectedDifficulty = AiDifficulty.HARD;
            } else {
                selectedDifficulty = AiDifficulty.MEDIUM;
            }
        });

        // Restore time selection
        if (selectedClockMillis == ChessClock.TWO_MINUTES_MILLIS) {
            rgTimeChoice.check(R.id.rb_time_2_min);
        } else {
            rgTimeChoice.check(R.id.rb_time_1_min);
        }
        rgTimeChoice.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_time_2_min) {
                selectedClockMillis = ChessClock.TWO_MINUTES_MILLIS;
            } else {
                selectedClockMillis = ChessClock.ONE_MINUTE_MILLIS;
            }
        });

        switchRotate.setChecked(prefs.isRotateOpponentPiecesEnabled());
        switchRotate.setOnCheckedChangeListener(
                (buttonView, isChecked) -> prefs.setRotateOpponentPiecesEnabled(isChecked)
        );

        btnStart.setOnClickListener(v -> {
            if (!isAdded()) {
                return;
            }
            var navController = Navigation.findNavController(v);
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getAction(R.id.action_setup_to_game) == null) {
                return;
            }
            Bundle gameArgs;
            if (singlePlayerMode) {
                boolean isRandom = COLOR_CHOICE_RANDOM.equals(selectedColorChoice);
                Color humanColor = resolveHumanColor(selectedColorChoice);
                gameArgs = GameFragment.createSinglePlayerArgs(
                        selectedClockMillis,
                        humanColor,
                        selectedDifficulty,
                        isRandom
                );
            } else {
                gameArgs = GameFragment.createArgs(selectedClockMillis);
            }
            navController.navigate(R.id.action_setup_to_game, gameArgs);
        });
    }

    @NonNull
    public static Color resolveHumanColor(@NonNull String colorChoice) {
        if (COLOR_CHOICE_BLACK.equals(colorChoice)) {
            return Color.BLACK;
        }
        if (COLOR_CHOICE_RANDOM.equals(colorChoice)) {
            return new Random().nextBoolean() ? Color.WHITE : Color.BLACK;
        }
        return Color.WHITE;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_TIME_MILLIS, selectedClockMillis);
        outState.putString(STATE_SELECTED_COLOR_CHOICE, selectedColorChoice);
        outState.putString(STATE_SELECTED_DIFFICULTY, selectedDifficulty.name());
    }
}
