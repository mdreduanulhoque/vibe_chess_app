package com.reduan.vibechess;

import static org.junit.Assert.assertEquals;

import com.reduan.vibechess.data.SettingsPreferences;

import org.junit.Test;

/**
 * Unit test verifying language tag constants used by SettingsPreferences.
 */
public class SettingsConstantsTest {

    @Test
    public void languageConstantsMatchSpec() {
        assertEquals("", SettingsPreferences.LANG_SYSTEM);
        assertEquals("en", SettingsPreferences.LANG_ENGLISH);
        assertEquals("bn", SettingsPreferences.LANG_BANGLA);
    }
}
