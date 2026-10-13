package com.ciphervault.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit tests verifying session persistence, synchronous logout cleanup,
 * token expiration validation, and relaunch behavior for com.ciphervault.app.SessionManager.
 */
public class SessionManagerTest {

    private FakeSharedPreferences fakePreferences;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        fakePreferences = new FakeSharedPreferences();
        sessionManager = new SessionManager(null, fakePreferences);
    }

    private String createTestJwt(String subject, long expSeconds) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + subject + "\",\"exp\":" + expSeconds + ",\"tokenVersion\":1}";
        String headerB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString("mock_signature".getBytes(StandardCharsets.UTF_8));
        return headerB64 + "." + payloadB64 + "." + sigB64;
    }

    @Test
    public void testInitialStateIsLoggedOut() {
        assertFalse("New session must not be logged in", sessionManager.isLoggedIn());
        assertNull("Token should be null initially", sessionManager.getToken());
        assertNull("Username should be null initially", sessionManager.getUsername());
        assertEquals("Default email should be fallback", "user@ciphervault.local", sessionManager.getEmail());
    }

    @Test
    public void testSaveLoginAndRetrieveAttributes() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
        String jwt = createTestJwt("user@test.local", futureExp);

        sessionManager.saveLogin(jwt, "testuser", "user@test.local", "Test User");

        assertTrue("User must be logged in after saveLogin", sessionManager.isLoggedIn());
        assertEquals("Token must match saved token", jwt, sessionManager.getToken());
        assertEquals("Username must match", "testuser", sessionManager.getUsername());
        assertEquals("Email must match", "user@test.local", sessionManager.getEmail());
        assertEquals("Name must match", "Test User", sessionManager.getName());
    }

    @Test
    public void testLogoutClearsActiveSessionAndGuaranteesLoggedOut() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
        String jwt = createTestJwt("user@test.local", futureExp);

        sessionManager.saveLogin(jwt, "testuser", "user@test.local", "Test User");
        assertTrue(sessionManager.isLoggedIn());

        sessionManager.logout();

        assertFalse("Must report logged out after logout", sessionManager.isLoggedIn());
        assertNull("Token must be null after logout", sessionManager.getToken());
        assertNull("Username must be null after logout", sessionManager.getUsername());
        assertFalse("Token must not remain in preferences", fakePreferences.contains(SessionManager.KEY_TOKEN));
        assertFalse("Username must not remain in preferences", fakePreferences.contains(SessionManager.KEY_USERNAME));
    }

    @Test
    public void testSessionRelaunchAfterLogoutRemainsLoggedOut() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
        String jwt = createTestJwt("user@test.local", futureExp);

        // 1. User logs in
        sessionManager.saveLogin(jwt, "testuser", "user@test.local", "Test User");
        assertTrue(sessionManager.isLoggedIn());

        // 2. User logs out
        sessionManager.logout();
        assertFalse(sessionManager.isLoggedIn());

        // 3. User closes and relaunches app (new SessionManager instance reading same disk storage)
        SessionManager relaunchedSessionManager = new SessionManager(null, fakePreferences);

        assertFalse("Relaunched app must require login after logout", relaunchedSessionManager.isLoggedIn());
        assertNull("Token must remain null after relaunch", relaunchedSessionManager.getToken());
        assertNull("Username must remain null after relaunch", relaunchedSessionManager.getUsername());
    }

    @Test
    public void testSessionRelaunchWithoutLogoutRemainsLoggedIn() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
        String jwt = createTestJwt("user@test.local", futureExp);

        // 1. User logs in and does NOT log out
        sessionManager.saveLogin(jwt, "activeuser", "active@test.local", "Active User");
        assertTrue(sessionManager.isLoggedIn());

        // 2. App restarts (new instance reading same storage)
        SessionManager relaunchedSessionManager = new SessionManager(null, fakePreferences);

        assertTrue("Relaunched app without logout must remain logged in", relaunchedSessionManager.isLoggedIn());
        assertEquals(jwt, relaunchedSessionManager.getToken());
        assertEquals("activeuser", relaunchedSessionManager.getUsername());
        assertEquals("Active User", relaunchedSessionManager.getName());
    }

    @Test
    public void testExpiredJwtFailsIsLoggedInAndAutoClears() {
        // Token expired 1 hour ago
        long pastExp = (System.currentTimeMillis() / 1000L) - 3600L;
        String expiredJwt = createTestJwt("expired@test.local", pastExp);

        sessionManager.saveLogin(expiredJwt, "expireduser", "expired@test.local", "Expired User");

        // isLoggedIn must detect expiration, invoke logout(), and return false
        assertFalse("Expired token must fail isLoggedIn", sessionManager.isLoggedIn());
        assertNull("Expired token must be cleared", sessionManager.getToken());
        assertFalse("Token must be removed from preferences", fakePreferences.contains(SessionManager.KEY_TOKEN));
    }

    @Test
    public void testIsTokenExpiredHelper() {
        assertTrue("Null token is expired", SessionManager.isTokenExpired(null));
        assertTrue("Empty token is expired", SessionManager.isTokenExpired(""));
        assertTrue("Whitespace token is expired", SessionManager.isTokenExpired("   "));

        // Opaque / non-JWT token (treated as valid in non-JWT mock scenarios)
        assertFalse("Opaque non-JWT token is not expired", SessionManager.isTokenExpired("opaque_mock_token"));

        // Expired JWT
        long pastExp = (System.currentTimeMillis() / 1000L) - 60L;
        String expiredJwt = createTestJwt("user@test.local", pastExp);
        assertTrue("Past exp JWT is expired", SessionManager.isTokenExpired(expiredJwt));

        // Unexpired JWT
        long futureExp = (System.currentTimeMillis() / 1000L) + 86400L;
        String validJwt = createTestJwt("user@test.local", futureExp);
        assertFalse("Future exp JWT is not expired", SessionManager.isTokenExpired(validJwt));
    }

    @Test
    public void testBiometricTogglePersistence() {
        assertFalse(sessionManager.isBiometricEnabled());

        sessionManager.setBiometricEnabled(true);
        assertTrue(sessionManager.isBiometricEnabled());

        SessionManager relaunched = new SessionManager(null, fakePreferences);
        assertTrue("Biometric setting should persist across relaunch", relaunched.isBiometricEnabled());

        sessionManager.setBiometricEnabled(false);
        assertFalse(sessionManager.isBiometricEnabled());
    }

    @Test
    public void testEmailExtractionFromJwtSubjectWhenEmailNotExplicitlySaved() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
        String jwt = createTestJwt("jwt_user@vault.io", futureExp);

        // Save login without explicit email
        sessionManager.saveLogin(jwt, "jwtuser");

        assertEquals("Email should be extracted from JWT sub claim", "jwt_user@vault.io", sessionManager.getEmail());
    }

    @Test
    public void testLegacyMigrationFlagPreventsResurrection() {
        // When user logs out, KEY_MIGRATION_COMPLETED flag is preserved
        sessionManager.logout();
        assertTrue("Migration flag should be set to true after logout to prevent resurrection",
                fakePreferences.getBoolean(SessionManager.KEY_MIGRATION_COMPLETED, false));

        // Attempting to read token or session state returns null / logged out
        SessionManager relaunched = new SessionManager(null, fakePreferences);
        assertFalse("Relaunched session manager must report logged out", relaunched.isLoggedIn());
        assertNull("Relaunched token must be null", relaunched.getToken());
    }

    @Test
    public void testExplicitRemoveAllSessionKeysOnLogout() {
        long futureExp = (System.currentTimeMillis() / 1000L) + 3600L;
        String jwt = createTestJwt("test@vault.io", futureExp);

        sessionManager.saveLogin(jwt, "vaultuser", "test@vault.io", "Vault User");
        sessionManager.setBiometricEnabled(true);

        assertTrue(fakePreferences.contains(SessionManager.KEY_TOKEN));
        assertTrue(fakePreferences.contains(SessionManager.KEY_USERNAME));
        assertTrue(fakePreferences.contains(SessionManager.KEY_NAME));
        assertTrue(fakePreferences.contains(SessionManager.KEY_EMAIL));
        assertTrue(fakePreferences.contains(SessionManager.KEY_BIOMETRIC));

        sessionManager.logout();

        assertFalse(fakePreferences.contains(SessionManager.KEY_TOKEN));
        assertFalse(fakePreferences.contains(SessionManager.KEY_USERNAME));
        assertFalse(fakePreferences.contains(SessionManager.KEY_NAME));
        assertFalse(fakePreferences.contains(SessionManager.KEY_EMAIL));
        assertFalse(fakePreferences.contains(SessionManager.KEY_BIOMETRIC));
    }

    /**
     * In-memory SharedPreferences for standalone unit testing without device dependencies.
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
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

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
