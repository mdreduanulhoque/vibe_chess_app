package com.reduan.vibechess.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.reduan.vibechess.data.SettingsPreferences;

/**
 * ViewModel for the Settings screen.
 * Exposes LiveData for Language, Dark Mode, Sound, Vibration, and Rotate Opponent Pieces,
 * persisting and applying changes immediately.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final SettingsPreferences preferences;
    private final MutableLiveData<Boolean> darkModeEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> languageTag = new MutableLiveData<>();
    private final MutableLiveData<Boolean> soundEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> vibrationEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> rotateOpponentPiecesEnabled = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.preferences = new SettingsPreferences(application);
        refreshFromPreferences();
    }

    public void refreshFromPreferences() {
        this.darkModeEnabled.setValue(preferences.isDarkModeEnabled());
        this.languageTag.setValue(preferences.getLanguageTag());
        this.soundEnabled.setValue(preferences.isSoundEnabled());
        this.vibrationEnabled.setValue(preferences.isVibrationEnabled());
        this.rotateOpponentPiecesEnabled.setValue(preferences.isRotateOpponentPiecesEnabled());
    }

    @NonNull
    public LiveData<Boolean> getDarkModeEnabled() {
        return darkModeEnabled;
    }

    @NonNull
    public LiveData<String> getLanguageTag() {
        return languageTag;
    }

    @NonNull
    public LiveData<Boolean> getSoundEnabled() {
        return soundEnabled;
    }

    @NonNull
    public LiveData<Boolean> getVibrationEnabled() {
        return vibrationEnabled;
    }

    @NonNull
    public LiveData<Boolean> getRotateOpponentPiecesEnabled() {
        return rotateOpponentPiecesEnabled;
    }

    public void setDarkModeEnabled(boolean enabled) {
        Boolean current = darkModeEnabled.getValue();
        if (current != null && current == enabled) {
            return;
        }
        darkModeEnabled.setValue(enabled);
        preferences.setDarkModeEnabled(enabled);
    }

    public void setLanguageTag(@NonNull String tag) {
        String current = languageTag.getValue();
        if (tag.equals(current)) {
            return;
        }
        languageTag.setValue(tag);
        preferences.setLanguageTag(tag);
    }

    public void setSoundEnabled(boolean enabled) {
        Boolean current = soundEnabled.getValue();
        if (current != null && current == enabled) {
            return;
        }
        soundEnabled.setValue(enabled);
        preferences.setSoundEnabled(enabled);
    }

    public void setVibrationEnabled(boolean enabled) {
        Boolean current = vibrationEnabled.getValue();
        if (current != null && current == enabled) {
            return;
        }
        vibrationEnabled.setValue(enabled);
        preferences.setVibrationEnabled(enabled);
    }

    public void setRotateOpponentPiecesEnabled(boolean enabled) {
        Boolean current = rotateOpponentPiecesEnabled.getValue();
        if (current != null && current == enabled) {
            return;
        }
        rotateOpponentPiecesEnabled.setValue(enabled);
        preferences.setRotateOpponentPiecesEnabled(enabled);
    }
}
