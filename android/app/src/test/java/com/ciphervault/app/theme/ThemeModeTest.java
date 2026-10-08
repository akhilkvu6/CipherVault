package com.ciphervault.app.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.appcompat.app.AppCompatDelegate;

import org.junit.Test;

/**
 * Unit tests verifying ThemeMode definition and deterministic behavior.
 */
public class ThemeModeTest {

    @Test
    public void testThemeModesExist() {
        assertNotNull("LIGHT mode must exist", ThemeMode.LIGHT);
        assertNotNull("DARK mode must exist", ThemeMode.DARK);
        assertNotNull("SYSTEM mode must exist", ThemeMode.SYSTEM);
        assertEquals("Exactly 3 theme modes should be defined", 3, ThemeMode.values().length);
    }

    @Test
    public void testNightModeMappingIsDeterministic() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.LIGHT.getNightMode());
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.DARK.getNightMode());
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.SYSTEM.getNightMode());
    }

    @Test
    public void testFromStringDeterministic() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("LIGHT"));
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("DARK"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("SYSTEM"));

        // Case insensitivity
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("light"));
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("dark"));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("system"));

        // Trim handling
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString("  LIGHT  "));

        // Null and invalid strings default safely to SYSTEM
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(null));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(""));
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("UNKNOWN_MODE"));
    }
}
