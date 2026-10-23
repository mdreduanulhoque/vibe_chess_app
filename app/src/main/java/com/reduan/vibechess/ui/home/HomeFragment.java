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
import com.google.android.material.snackbar.Snackbar;
import com.reduan.vibechess.R;

/**
 * Home screen with 4 buttons: Single Player, Multiplayer, How to Play, and Settings.
 * Game modes and How to Play are placeholders in Step 1; Settings opens the Settings screen.
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

        View.OnClickListener placeholderListener = v ->
                Snackbar.make(v, R.string.placeholder_coming_soon, Snackbar.LENGTH_SHORT).show();

        btnSinglePlayer.setOnClickListener(placeholderListener);
        btnMultiplayer.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_setup));
        btnHowToPlay.setOnClickListener(placeholderListener);

        btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_settings));
    }
}
