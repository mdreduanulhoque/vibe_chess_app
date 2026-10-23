package com.reduan.vibechess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.data.SettingsPreferences;
import com.reduan.vibechess.ui.setup.GameSetupFragment;

import org.junit.Test;

/**
 * Unit test verifying language tag constants and setup color resolution.
 */
public class SettingsConstantsTest {

    @Test
    public void languageConstantsMatchSpec() {
        assertEquals("", SettingsPreferences.LANG_SYSTEM);
        assertEquals("en", SettingsPreferences.LANG_ENGLISH);
        assertEquals("bn", SettingsPreferences.LANG_BANGLA);
    }

    @Test
    public void setupColorResolutionMatchesChoice() {
        assertEquals(Color.WHITE, GameSetupFragment.resolveHumanColor(GameSetupFragment.COLOR_CHOICE_WHITE));
        assertEquals(Color.BLACK, GameSetupFragment.resolveHumanColor(GameSetupFragment.COLOR_CHOICE_BLACK));
        assertNotNull(GameSetupFragment.resolveHumanColor(GameSetupFragment.COLOR_CHOICE_RANDOM));
    }
}
