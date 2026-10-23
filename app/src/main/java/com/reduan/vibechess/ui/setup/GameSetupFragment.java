package com.reduan.vibechess.ui.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.reduan.vibechess.R;
import com.reduan.vibechess.core.ChessClock;
import com.reduan.vibechess.data.SettingsPreferences;
import com.reduan.vibechess.ui.game.GameFragment;

/**
 * Game Setup screen for Multiplayer (Pass-and-Play):
 * lets the user choose 1 minute or 2 minutes per side and toggle rotating opponent's pieces 180°.
 */
public class GameSetupFragment extends Fragment {

    private static final String STATE_SELECTED_TIME_MILLIS = "selected_time_millis";

    private long selectedClockMillis = ChessClock.ONE_MINUTE_MILLIS;

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

        if (savedInstanceState != null) {
            selectedClockMillis = savedInstanceState.getLong(
                    STATE_SELECTED_TIME_MILLIS,
                    ChessClock.ONE_MINUTE_MILLIS
            );
        }

        SettingsPreferences prefs = new SettingsPreferences(requireContext());

        MaterialButton btnBack = view.findViewById(R.id.btn_setup_back);
        RadioGroup rgTimeChoice = view.findViewById(R.id.rg_time_choice);
        SwitchMaterial switchRotate = view.findViewById(R.id.switch_setup_rotate_opponent);
        MaterialButton btnStart = view.findViewById(R.id.btn_start_game);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

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
            Bundle args = GameFragment.createArgs(selectedClockMillis);
            Navigation.findNavController(v).navigate(R.id.action_setup_to_game, args);
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SELECTED_TIME_MILLIS, selectedClockMillis);
    }
}
