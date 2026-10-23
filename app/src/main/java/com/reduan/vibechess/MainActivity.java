package com.reduan.vibechess;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.reduan.vibechess.data.SettingsPreferences;

/**
 * Single host activity for Vibe Chess navigation graph.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SettingsPreferences preferences = new SettingsPreferences(this);
        SettingsPreferences.applyTheme(preferences.isDarkModeEnabled());
        SettingsPreferences.applyLanguage(preferences.getLanguageTag());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
