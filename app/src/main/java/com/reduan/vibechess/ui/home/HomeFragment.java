package com.reduan.vibechess.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.reduan.vibechess.R;
import com.reduan.vibechess.ui.setup.GameSetupFragment;

/**
 * Home screen with 4 navigation actions: Single Player, Multiplayer, How to Play, and Settings.
 * Minimalist orange/white UIU-inspired brand look supporting both portrait and landscape.
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnSinglePlayer = view.findViewById(R.id.btn_single_player);
        MaterialButton btnMultiplayer = view.findViewById(R.id.btn_multiplayer);
        MaterialButton btnHowToPlay = view.findViewById(R.id.btn_how_to_play);
        MaterialButton btnSettings = view.findViewById(R.id.btn_settings);

        btnSinglePlayer.setOnClickListener(v -> {
            Bundle args = GameSetupFragment.createArgs(true);
            Navigation.findNavController(v).navigate(R.id.action_home_to_setup, args);
        });

        btnMultiplayer.setOnClickListener(v -> {
            Bundle args = GameSetupFragment.createArgs(false);
            Navigation.findNavController(v).navigate(R.id.action_home_to_setup, args);
        });

        btnHowToPlay.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_how_to_play));

        btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_settings));
    }
}
