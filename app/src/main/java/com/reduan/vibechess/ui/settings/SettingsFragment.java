package com.reduan.vibechess.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.reduan.vibechess.R;
import com.reduan.vibechess.data.SettingsPreferences;

/**
 * Minimal Settings screen with Language (English / বাংলা / System) and Dark mode switch.
 * Both settings apply instantaneously and persist across restarts.
 */
public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;
    private boolean isUpdatingUi = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        MaterialButton btnBack = view.findViewById(R.id.btn_settings_back);
        RadioGroup rgLanguage = view.findViewById(R.id.rg_language);
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        viewModel.getLanguageTag().observe(getViewLifecycleOwner(), tag -> {
            isUpdatingUi = true;
            int targetId;
            if (SettingsPreferences.LANG_ENGLISH.equals(tag)) {
                targetId = R.id.rb_lang_en;
            } else if (SettingsPreferences.LANG_BANGLA.equals(tag)) {
                targetId = R.id.rb_lang_bn;
            } else {
                targetId = R.id.rb_lang_system;
            }
            if (rgLanguage.getCheckedRadioButtonId() != targetId) {
                rgLanguage.check(targetId);
            }
            isUpdatingUi = false;
        });

        viewModel.getDarkModeEnabled().observe(getViewLifecycleOwner(), enabled -> {
            isUpdatingUi = true;
            if (switchDarkMode.isChecked() != enabled) {
                switchDarkMode.setChecked(enabled);
            }
            isUpdatingUi = false;
        });

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (isUpdatingUi) {
                return;
            }
            if (checkedId == R.id.rb_lang_en) {
                viewModel.setLanguageTag(SettingsPreferences.LANG_ENGLISH);
            } else if (checkedId == R.id.rb_lang_bn) {
                viewModel.setLanguageTag(SettingsPreferences.LANG_BANGLA);
            } else if (checkedId == R.id.rb_lang_system) {
                viewModel.setLanguageTag(SettingsPreferences.LANG_SYSTEM);
            }
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingUi) {
                return;
            }
            viewModel.setDarkModeEnabled(isChecked);
        });
    }
}
