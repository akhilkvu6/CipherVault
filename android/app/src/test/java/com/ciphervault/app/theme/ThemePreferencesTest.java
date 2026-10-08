package com.ciphervault.app.theme;

import static org.junit.Assert.assertEquals;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests verifying ThemePreferences read/write and default mode.
 */
public class ThemePreferencesTest {

    private FakeSharedPreferences fakePreferences;
    private ThemePreferences themePreferences;

    @Before
    public void setUp() {
        fakePreferences = new FakeSharedPreferences();
        themePreferences = new ThemePreferences(fakePreferences);
    }

    @Test
    public void testDefaultModeIsSystem() {
        assertEquals("Default theme mode must be SYSTEM", ThemeMode.SYSTEM, themePreferences.getThemeMode());
        assertEquals("Default constant must be SYSTEM", ThemeMode.SYSTEM, ThemePreferences.DEFAULT_THEME_MODE);
    }

    @Test
    public void testPreferenceReadWriteWorks() {
        // Set and get LIGHT
        themePreferences.setThemeMode(ThemeMode.LIGHT);
        assertEquals(ThemeMode.LIGHT, themePreferences.getThemeMode());
        assertEquals("LIGHT", fakePreferences.getString(ThemePreferences.KEY_THEME_MODE, null));

        // Set and get DARK
        themePreferences.setThemeMode(ThemeMode.DARK);
        assertEquals(ThemeMode.DARK, themePreferences.getThemeMode());
        assertEquals("DARK", fakePreferences.getString(ThemePreferences.KEY_THEME_MODE, null));

        // Set and get SYSTEM
        themePreferences.setThemeMode(ThemeMode.SYSTEM);
        assertEquals(ThemeMode.SYSTEM, themePreferences.getThemeMode());
        assertEquals("SYSTEM", fakePreferences.getString(ThemePreferences.KEY_THEME_MODE, null));
    }

    @Test
    public void testSetNullDefaultsToSystem() {
        themePreferences.setThemeMode(ThemeMode.LIGHT);
        assertEquals(ThemeMode.LIGHT, themePreferences.getThemeMode());

        themePreferences.setThemeMode(null);
        assertEquals(ThemeMode.SYSTEM, themePreferences.getThemeMode());
    }

    @Test
    public void testDeterministicRepeatedReads() {
        themePreferences.setThemeMode(ThemeMode.DARK);
        for (int i = 0; i < 5; i++) {
            assertEquals(ThemeMode.DARK, themePreferences.getThemeMode());
        }
    }

    /**
     * In-memory FakeSharedPreferences for fast, host-side JVM unit testing.
     */
    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> storage = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(storage);
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            Object val = storage.get(key);
            return (val instanceof String) ? (String) val : defValue;
        }

        @Nullable
        @Override
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object val = storage.get(key);
            return (val instanceof Integer) ? (Integer) val : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object val = storage.get(key);
            return (val instanceof Long) ? (Long) val : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object val = storage.get(key);
            return (val instanceof Float) ? (Float) val : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object val = storage.get(key);
            return (val instanceof Boolean) ? (Boolean) val : defValue;
        }

        @Override
        public boolean contains(String key) {
            return storage.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        private class FakeEditor implements Editor {
            private final Map<String, Object> pending = new HashMap<>();

            @Override
            public Editor putString(String key, @Nullable String value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, @Nullable Set<String> values) {
                pending.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                pending.remove(key);
                storage.remove(key);
                return this;
            }

            @Override
            public Editor clear() {
                pending.clear();
                storage.clear();
                return this;
            }

            @Override
            public boolean commit() {
                storage.putAll(pending);
                return true;
            }

            @Override
            public void apply() {
                storage.putAll(pending);
            }
        }
    }
}
