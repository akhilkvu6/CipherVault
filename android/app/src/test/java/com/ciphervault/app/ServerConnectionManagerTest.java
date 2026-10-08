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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Forensic unit test suite for CipherVault ServerConnectionManager.
 * Tests 5-server capacity limit, LRU ordering, eviction, duplicate prevention,
 * endpoint normalization, priority selection, and disconnect safety.
 */
public class ServerConnectionManagerTest {

    private FakeSharedPreferences fakePrefs;
    private ServerConnectionManager manager;

    @Before
    public void setUp() {
        fakePrefs = new FakeSharedPreferences();
        manager = new ServerConnectionManager(fakePrefs);
    }

    @Test
    public void testEmptyInitialState() {
        List<SavedServer> list = manager.getSavedServers();
        assertTrue("Initially saved servers list must be empty", list.isEmpty());
    }

    @Test
    public void testEndpointNormalization() {
        SavedServer s1 = manager.recordServerUsed("http://10.0.0.5:8080");
        SavedServer s2 = manager.recordServerUsed("http://10.0.0.5:8080/");
        SavedServer s3 = manager.recordServerUsed("10.0.0.5:8080");

        List<SavedServer> list = manager.getSavedServers();
        assertEquals("Duplicate variations must normalize to single entry", 1, list.size());
        assertEquals("http://10.0.0.5:8080/", list.get(0).getCanonicalUrl());
        assertEquals("10.0.0.5:8080", list.get(0).getDisplayAddress());
    }

    @Test
    public void testLruOrderingAndEviction_ExactSpecification() {
        // Step 1: Add A, B, C, D, E sequentially
        // Each new addition is placed at #1 (index 0)
        manager.recordServerUsed("http://10.0.0.1:8080"); // A
        manager.recordServerUsed("http://10.0.0.2:8080"); // B
        manager.recordServerUsed("http://10.0.0.3:8080"); // C
        manager.recordServerUsed("http://10.0.0.4:8080"); // D
        manager.recordServerUsed("http://10.0.0.5:8080"); // E

        List<SavedServer> list = manager.getSavedServers();
        assertEquals(5, list.size());
        assertEquals("http://10.0.0.5:8080/", list.get(0).getCanonicalUrl()); // E
        assertEquals("http://10.0.0.4:8080/", list.get(1).getCanonicalUrl()); // D
        assertEquals("http://10.0.0.3:8080/", list.get(2).getCanonicalUrl()); // C
        assertEquals("http://10.0.0.2:8080/", list.get(3).getCanonicalUrl()); // B
        assertEquals("http://10.0.0.1:8080/", list.get(4).getCanonicalUrl()); // A

        // Step 2: Use C -> C moves to #1
        manager.recordServerUsed("http://10.0.0.3:8080");
        list = manager.getSavedServers();
        assertEquals(5, list.size());
        assertEquals("http://10.0.0.3:8080/", list.get(0).getCanonicalUrl()); // C
        assertEquals("http://10.0.0.5:8080/", list.get(1).getCanonicalUrl()); // E
        assertEquals("http://10.0.0.4:8080/", list.get(2).getCanonicalUrl()); // D
        assertEquals("http://10.0.0.2:8080/", list.get(3).getCanonicalUrl()); // B
        assertEquals("http://10.0.0.1:8080/", list.get(4).getCanonicalUrl()); // A

        // Step 3: Use E -> E moves to #1
        manager.recordServerUsed("http://10.0.0.5:8080");
        list = manager.getSavedServers();
        assertEquals(5, list.size());
        assertEquals("http://10.0.0.5:8080/", list.get(0).getCanonicalUrl()); // E
        assertEquals("http://10.0.0.3:8080/", list.get(1).getCanonicalUrl()); // C
        assertEquals("http://10.0.0.4:8080/", list.get(2).getCanonicalUrl()); // D
        assertEquals("http://10.0.0.2:8080/", list.get(3).getCanonicalUrl()); // B
        assertEquals("http://10.0.0.1:8080/", list.get(4).getCanonicalUrl()); // A

        // Step 4: Add new server F -> F added at #1, A (least recently used) evicted!
        manager.recordServerUsed("http://10.0.0.6:8080"); // F
        list = manager.getSavedServers();
        assertEquals("Max capacity 5 must be preserved", 5, list.size());
        assertEquals("http://10.0.0.6:8080/", list.get(0).getCanonicalUrl()); // F
        assertEquals("http://10.0.0.5:8080/", list.get(1).getCanonicalUrl()); // E
        assertEquals("http://10.0.0.3:8080/", list.get(2).getCanonicalUrl()); // C
        assertEquals("http://10.0.0.4:8080/", list.get(3).getCanonicalUrl()); // D
        assertEquals("http://10.0.0.2:8080/", list.get(4).getCanonicalUrl()); // B

        // Verify A was evicted
        for (SavedServer s : list) {
            assertFalse("A (10.0.0.1) should have been evicted", s.getCanonicalUrl().contains("10.0.0.1"));
        }
    }

    @Test
    public void testDuplicatePrevention() {
        manager.recordServerUsed("http://10.180.140.219:8080");
        manager.recordServerUsed("http://192.168.1.100:8080");
        manager.recordServerUsed("http://10.0.2.2:8080");

        assertEquals(3, manager.getSavedServers().size());

        // Connect to existing server again
        manager.recordServerUsed("http://192.168.1.100:8080");

        List<SavedServer> list = manager.getSavedServers();
        assertEquals("Duplicate must not increase list count", 3, list.size());
        assertEquals("Existing server must move to #1", "http://192.168.1.100:8080/", list.get(0).getCanonicalUrl());
    }

    @Test
    public void testPrioritySelection_CriticalCases() {
        // Setup LRU list: 1=A, 2=B, 3=C, 4=D, 5=E
        manager.recordServerUsed("http://10.0.0.1:8080"); // A (LRU #5)
        manager.recordServerUsed("http://10.0.0.2:8080"); // B (LRU #4)
        manager.recordServerUsed("http://10.0.0.3:8080"); // C (LRU #3)
        manager.recordServerUsed("http://10.0.0.4:8080"); // D (LRU #2)
        manager.recordServerUsed("http://10.0.0.5:8080"); // E (LRU #1)

        List<SavedServer> servers = manager.getSavedServers();
        // Server order: E (#1), D (#2), C (#3), B (#4), A (#5)

        // Case 1: Probes result: E=UP, D=DOWN, C=UP, B=DOWN, A=UP
        // E has highest priority (#1) -> E selected!
        Map<String, Boolean> reachability1 = new HashMap<>();
        reachability1.put("http://10.0.0.5:8080/", true);  // E: UP
        reachability1.put("http://10.0.0.4:8080/", false); // D: DOWN
        reachability1.put("http://10.0.0.3:8080/", true);  // C: UP
        reachability1.put("http://10.0.0.2:8080/", false); // B: DOWN
        reachability1.put("http://10.0.0.1:8080/", true);  // A: UP

        SavedServer selected1 = ServerConnectionManager.selectHighestPriorityReachable(servers, reachability1);
        assertNotNull(selected1);
        assertEquals("http://10.0.0.5:8080/", selected1.getCanonicalUrl());

        // Case 2: Probes result: E=DOWN, D=DOWN, C=UP, B=UP, A=DOWN
        // C has higher LRU priority (#3) than B (#4) -> C selected!
        Map<String, Boolean> reachability2 = new HashMap<>();
        reachability2.put("http://10.0.0.5:8080/", false); // E: DOWN
        reachability2.put("http://10.0.0.4:8080/", false); // D: DOWN
        reachability2.put("http://10.0.0.3:8080/", true);  // C: UP
        reachability2.put("http://10.0.0.2:8080/", true);  // B: UP
        reachability2.put("http://10.0.0.1:8080/", false); // A: DOWN

        SavedServer selected2 = ServerConnectionManager.selectHighestPriorityReachable(servers, reachability2);
        assertNotNull(selected2);
        assertEquals("http://10.0.0.3:8080/", selected2.getCanonicalUrl());

        // Case 3: All 5 fail
        Map<String, Boolean> reachability3 = new HashMap<>();
        reachability3.put("http://10.0.0.5:8080/", false);
        reachability3.put("http://10.0.0.4:8080/", false);
        reachability3.put("http://10.0.0.3:8080/", false);
        reachability3.put("http://10.0.0.2:8080/", false);
        reachability3.put("http://10.0.0.1:8080/", false);

        SavedServer selected3 = ServerConnectionManager.selectHighestPriorityReachable(servers, reachability3);
        assertNull("When all servers fail, selected active server must be null", selected3);
    }

    @Test
    public void testFailedServersRetained() {
        manager.recordServerUsed("http://10.0.0.1:8080");
        manager.recordServerUsed("http://10.0.0.2:8080");

        // Simulating failed probes
        Map<String, Boolean> reachability = new HashMap<>();
        reachability.put("http://10.0.0.1:8080/", false);
        reachability.put("http://10.0.0.2:8080/", false);

        // Verification: Servers must remain in storage even after failure
        List<SavedServer> retained = manager.getSavedServers();
        assertEquals("Failed servers must remain saved in storage", 2, retained.size());
    }

    @Test
    public void testForgetServer() {
        manager.recordServerUsed("http://10.0.0.1:8080");
        manager.recordServerUsed("http://10.0.0.2:8080");

        assertEquals(2, manager.getSavedServers().size());

        boolean removed = manager.forgetServer("http://10.0.0.1:8080/");
        assertTrue("Server should be removed", removed);

        List<SavedServer> remaining = manager.getSavedServers();
        assertEquals(1, remaining.size());
        assertEquals("http://10.0.0.2:8080/", remaining.get(0).getCanonicalUrl());
    }

    @Test
    public void testCorruptedOversizedListTruncatedToFive() {
        // Manually inject 8 servers to simulate corrupted external state
        StringBuilder json = new StringBuilder("[");
        for (int i = 1; i <= 8; i++) {
            json.append("{\"scheme\":\"http\",\"host\":\"10.0.0.").append(i)
                    .append("\",\"port\":8080,\"lastUsed\":").append(i * 1000).append("}");
            if (i < 8) json.append(",");
        }
        json.append("]");

        fakePrefs.edit().putString("saved_servers_lru_json", json.toString()).apply();

        List<SavedServer> truncated = manager.getSavedServers();
        assertEquals("Corrupted list must be truncated to exactly 5", 5, truncated.size());
        // Highest lastUsed was i=8 (8000), should be at index 0
        assertEquals("10.0.0.8:8080", truncated.get(0).getDisplayAddress());
    }

    @Test
    public void testDisconnectKeepsSavedServers() {
        manager.recordServerUsed("http://10.0.0.1:8080");
        manager.recordServerUsed("http://10.0.0.2:8080");
        assertEquals(2, manager.getSavedServers().size());

        // Disconnect
        manager.disconnect();

        // Saved servers must NOT be deleted
        List<SavedServer> afterDisconnect = manager.getSavedServers();
        assertEquals("Disconnect must preserve all saved servers", 2, afterDisconnect.size());
        assertEquals("http://10.0.0.2:8080/", afterDisconnect.get(0).getCanonicalUrl());
        assertEquals("http://10.0.0.1:8080/", afterDisconnect.get(1).getCanonicalUrl());
    }

    /**
     * In-memory implementation of SharedPreferences for hermetic JVM testing.
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
            Object val = storage.get(key);
            return (val instanceof Set) ? (Set<String>) val : defValues;
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
