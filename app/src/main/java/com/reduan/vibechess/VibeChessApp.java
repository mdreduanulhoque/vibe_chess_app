package com.reduan.vibechess;

import android.app.Application;

import com.reduan.vibechess.data.SettingsPreferences;

/**
 * Application entry point for Vibe Chess.
 * Restores persisted theme mode on process startup.
 */
public class VibeChessApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsPreferences preferences = new SettingsPreferences(this);
        SettingsPreferences.applyTheme(preferences.isDarkModeEnabled());
    }
}
