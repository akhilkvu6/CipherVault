package com.ciphervault.app.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.ciphervault.app.core.security.KeyStoreCipher;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests verifying SessionManager and direct Keystore encryption (Section 43).
 */
public class SessionManagerTest {

    private FakeSharedPreferences fakePreferences;
    private KeyStoreCipher keyStoreCipher;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        fakePreferences = new FakeSharedPreferences();
        keyStoreCipher = new KeyStoreCipher();
        sessionManager = new SessionManager(fakePreferences, keyStoreCipher);
    }

    @Test
    public void testEmptySession() {
        assertFalse("New session manager must not have session", sessionManager.hasSession());
        assertNull("Token should be null for empty session", sessionManager.getToken());
        assertNull("Username should be null for empty session", sessionManager.getUsername());
        assertNull("Name should be null for empty session", sessionManager.getName());
    }

    @Test
    public void testSaveAndReadSession() {
        String testJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-IDcSemACt8x4iTMCda8Yhe3iZaWbvV5XKSTbuAn0M";
        String testUsername = "akhil";
        String testName = "Akhil Krishna";

        sessionManager.saveSession(testJwt, testUsername, testName);

        assertTrue("hasSession should be true after save", sessionManager.hasSession());
        assertEquals("Decrypted token must match original JWT", testJwt, sessionManager.getToken());
        assertEquals("Username must match", testUsername, sessionManager.getUsername());
        assertEquals("Name must match", testName, sessionManager.getName());

        // Verify stored token is encrypted, not plaintext
        String rawStoredToken = fakePreferences.getString("key_encrypted_token", null);
        assertNotNull("Stored token must exist in preferences", rawStoredToken);
        assertFalse("Stored token MUST NOT be plaintext JWT", rawStoredToken.equals(testJwt));
    }

    @Test
    public void testClearSession() {
        sessionManager.saveSession("sample-token", "user1", "User One");
        assertTrue(sessionManager.hasSession());

        sessionManager.clearSession();

        assertFalse("hasSession must be false after clear", sessionManager.hasSession());
        assertNull("Token must be null after clear", sessionManager.getToken());
        assertNull("Username must be null after clear", sessionManager.getUsername());
        assertNull("Name must be null after clear", sessionManager.getName());
    }

    @Test
    public void testSessionInvalidationNotification() {
        final boolean[] notified = {false};
        sessionManager.setOnSessionInvalidatedListener(() -> notified[0] = true);

        sessionManager.saveSession("active-token", "user", "name");
        assertTrue(sessionManager.hasSession());

        sessionManager.notifySessionInvalidated();

        assertTrue("Invalidation listener must be called", notified[0]);
        assertFalse("Session must be cleared on invalidation", sessionManager.hasSession());
    }

    /**
     * In-memory SharedPreferences for unit testing without device dependencies.
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
