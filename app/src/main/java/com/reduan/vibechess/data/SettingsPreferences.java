package com.reduan.vibechess.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Manages persisted user settings (Language, Dark mode, Sound, Vibration,
 * and Rotate opponent's pieces 180°) using SharedPreferences and applies
 * theme/locale changes immediately via {@link AppCompatDelegate}.
 */
public class SettingsPreferences {

    private static final String PREFS_NAME = "vibe_chess_settings";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";
    private static final String KEY_LANGUAGE_TAG = "language_tag";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_ROTATE_OPPONENT_PIECES = "rotate_opponent_pieces";
    private static final String KEY_HAS_SEEN_HOW_TO_PLAY = "has_seen_how_to_play";

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
     * Returns whether sound effects are enabled (default true).
     */
    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true);
    }

    /**
     * Persists whether sound effects are enabled.
     */
    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    /**
     * Returns whether haptic vibration feedback is enabled (default true).
     */
    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true);
    }

    /**
     * Persists whether haptic vibration feedback is enabled.
     */
    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }

    /**
     * Returns whether opponent (Black) pieces should be rotated 180 degrees in pass-and-play.
     * Default is true per PROJECT_SPEC.md.
     */
    public boolean isRotateOpponentPiecesEnabled() {
        return prefs.getBoolean(KEY_ROTATE_OPPONENT_PIECES, true);
    }

    /**
     * Persists whether opponent (Black) pieces should be rotated 180 degrees.
     */
    public void setRotateOpponentPiecesEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_ROTATE_OPPONENT_PIECES, enabled).apply();
    }

    /**
     * Returns whether the user has already seen the "How to Play" tutorial on first launch.
     */
    public boolean hasSeenHowToPlay() {
        return prefs.getBoolean(KEY_HAS_SEEN_HOW_TO_PLAY, false);
    }

    /**
     * Persists that the user has seen or skipped the first-launch "How to Play" screen.
     */
    public void setHasSeenHowToPlay(boolean seen) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_HOW_TO_PLAY, seen).apply();
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
