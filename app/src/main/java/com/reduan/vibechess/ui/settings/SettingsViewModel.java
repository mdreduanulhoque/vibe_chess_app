package com.reduan.vibechess.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.reduan.vibechess.data.SettingsPreferences;

/**
 * ViewModel for the Settings screen.
 * Exposes LiveData for Dark Mode and Language selection and persists changes immediately.
 */
public class SettingsViewModel extends AndroidViewModel {

    private final SettingsPreferences preferences;
    private final MutableLiveData<Boolean> darkModeEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> languageTag = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        this.preferences = new SettingsPreferences(application);
        this.darkModeEnabled.setValue(preferences.isDarkModeEnabled());
        this.languageTag.setValue(preferences.getLanguageTag());
    }

    @NonNull
    public LiveData<Boolean> getDarkModeEnabled() {
        return darkModeEnabled;
    }

    @NonNull
    public LiveData<String> getLanguageTag() {
        return languageTag;
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
}
