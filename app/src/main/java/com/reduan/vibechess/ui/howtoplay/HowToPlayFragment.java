package com.reduan.vibechess.ui.howtoplay;

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

/**
 * How to Play screen presenting Vibe Chess rules:
 * no check / no checkmate, capturable kings (7 points), piece point values,
 * 1/2-minute chess clock timeout victory, and the live winning probability bar.
 */
public class HowToPlayFragment extends Fragment {

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

        MaterialButton btnBack = view.findViewById(R.id.btn_how_to_play_back);
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }
}
