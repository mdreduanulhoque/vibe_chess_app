package com.reduan.vibechess.ui.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reduan.vibechess.R;
import com.reduan.vibechess.data.SettingsPreferences;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lightweight game sound effects (move, capture, low-time tick, game end)
 * via {@link SoundPool} and capture haptic feedback, respecting the user's
 * persisted Sound and Vibration toggles in {@link SettingsPreferences}.
 */
public final class GameSoundManager {

    private static final long CAPTURE_VIBRATION_MS = 35L;

    private final Context appContext;
    private final SettingsPreferences settingsPreferences;
    @Nullable
    private SoundPool soundPool;
    private final Set<Integer> loadedSamples = ConcurrentHashMap.newKeySet();

    private int moveSoundId = 0;
    private int captureSoundId = 0;
    private int lowTimeSoundId = 0;
    private int gameEndSoundId = 0;

    public GameSoundManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.settingsPreferences = new SettingsPreferences(appContext);
        initSoundPool();
    }

    private void initSoundPool() {
        try {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            SoundPool pool = new SoundPool.Builder()
                    .setMaxStreams(4)
                    .setAudioAttributes(audioAttributes)
                    .build();

            pool.setOnLoadCompleteListener((sp, sampleId, status) -> {
                if (status == 0) {
                    loadedSamples.add(sampleId);
                }
            });

            moveSoundId = pool.load(appContext, R.raw.sfx_move, 1);
            captureSoundId = pool.load(appContext, R.raw.sfx_capture, 1);
            lowTimeSoundId = pool.load(appContext, R.raw.sfx_low_time, 1);
            gameEndSoundId = pool.load(appContext, R.raw.sfx_game_end, 1);
            this.soundPool = pool;
        } catch (RuntimeException ignored) {
            this.soundPool = null;
        }
    }

    /**
     * Plays the quiet move sound if Sound is enabled in settings.
     */
    public void playMove() {
        playSample(moveSoundId, 0.85f);
    }

    /**
     * Plays the capture sound (if Sound is enabled) and triggers haptic feedback
     * (if Vibration is enabled).
     */
    public void playCaptureAndHaptics(@Nullable View view) {
        playSample(captureSoundId, 1.0f);
        triggerCaptureHaptics(view);
    }

    /**
     * Plays the subtle low-time clock tick (last 10 seconds) if Sound is enabled.
     */
    public void playLowTimeTick() {
        playSample(lowTimeSoundId, 0.65f);
    }

    /**
     * Plays the game-end chime if Sound is enabled.
     */
    public void playGameEnd() {
        playSample(gameEndSoundId, 1.0f);
    }

    private void playSample(int soundId, float volume) {
        if (!settingsPreferences.isSoundEnabled()) {
            return;
        }
        SoundPool pool = this.soundPool;
        if (pool != null && soundId != 0 && loadedSamples.contains(soundId)) {
            pool.play(soundId, volume, volume, 1, 0, 1.0f);
        }
    }

    private void triggerCaptureHaptics(@Nullable View view) {
        if (!settingsPreferences.isVibrationEnabled()) {
            return;
        }
        try {
            Vibrator vibrator = getVibrator();
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(
                        CAPTURE_VIBRATION_MS,
                        VibrationEffect.DEFAULT_AMPLITUDE
                ));
                return;
            }
        } catch (RuntimeException ignored) {
            // Fall back to view haptic feedback if vibrator service is unavailable
        }
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    @Nullable
    private Vibrator getVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Releases native SoundPool resources when the game screen is destroyed.
     */
    public void release() {
        loadedSamples.clear();
        SoundPool pool = this.soundPool;
        this.soundPool = null;
        if (pool != null) {
            pool.setOnLoadCompleteListener(null);
            pool.autoPause();
            pool.release();
        }
    }
}
