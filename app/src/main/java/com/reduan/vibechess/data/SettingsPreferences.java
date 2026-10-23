package com.reduan.vibechess.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Manages persisted user settings (Dark mode and Language) using SharedPreferences
 * and applies them at runtime via {@link AppCompatDelegate}.
 */
public class SettingsPreferences {

    private static final String PREFS_NAME = "vibe_chess_settings";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_LANGUAGE_TAG = "language_tag";

    public static final String LANG_SYSTEM = "";
    public static final String LANG_ENGLISH = "en";
    public static final String LANG_BANGLA = "bn";

    private final SharedPreferences prefs;

    public SettingsPreferences(@NonNull Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns whether dark mode is enabled. Light mode (false) is the default per spec.
     */
    public boolean isDarkModeEnabled() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * Persists and immediately applies the dark mode setting.
     */
    public void setDarkModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        applyTheme(enabled);
    }

    /**
     * Returns the persisted language tag ("", "en", or "bn").
     */
    @NonNull
    public String getLanguageTag() {
        String saved = prefs.getString(KEY_LANGUAGE_TAG, null);
        if (saved != null) {
            return saved;
        }
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        if (current.isEmpty()) {
            return LANG_SYSTEM;
        }
        String tags = current.toLanguageTags();
        if (tags.startsWith(LANG_BANGLA)) {
            return LANG_BANGLA;
        } else if (tags.startsWith(LANG_ENGLISH)) {
            return LANG_ENGLISH;
        }
        return LANG_SYSTEM;
    }

    /**
     * Persists and immediately applies the selected language tag ("", "en", or "bn").
     */
    public void setLanguageTag(@NonNull String languageTag) {
        prefs.edit().putString(KEY_LANGUAGE_TAG, languageTag).apply();
        applyLanguage(languageTag);
    }

    /**
     * Applies the night mode setting via AppCompatDelegate if it differs from current mode.
     */
    public static void applyTheme(boolean darkModeEnabled) {
        int targetMode = darkModeEnabled
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    /**
     * Applies the application locale via AppCompatDelegate if it differs from current locales.
     */
    public static void applyLanguage(@NonNull String languageTag) {
        LocaleListCompat targetLocales = languageTag.isEmpty()
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(languageTag);
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (!targetLocales.equals(currentLocales)) {
            AppCompatDelegate.setApplicationLocales(targetLocales);
        }
    }
}
