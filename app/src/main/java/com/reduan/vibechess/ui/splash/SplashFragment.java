package com.reduan.vibechess.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.reduan.vibechess.R;
import com.reduan.vibechess.data.SettingsPreferences;
import com.reduan.vibechess.ui.howtoplay.HowToPlayFragment;

/**
 * Minimalist splash screen displaying the "Vibe Chess" brand mark on orange/white
 * before transitioning to the "How to Play" tutorial on first launch or the Home screen
 * on subsequent launches.
 */
public class SplashFragment extends Fragment {

    private static final long SPLASH_DELAY_MS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable navigateRunnable = () -> {
        View view = getView();
        if (view != null && isAdded()) {
            NavController navController = Navigation.findNavController(view);
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == R.id.splashFragment) {
                SettingsPreferences prefs = new SettingsPreferences(requireContext());
                if (!prefs.hasSeenHowToPlay()) {
                    prefs.setHasSeenHowToPlay(true);
                    Bundle args = HowToPlayFragment.createArgs(true);
                    navController.navigate(R.id.action_splash_to_how_to_play, args);
                } else {
                    navController.navigate(R.id.action_splash_to_home);
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handler.postDelayed(navigateRunnable, SPLASH_DELAY_MS);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(navigateRunnable);
        super.onDestroyView();
    }
}
