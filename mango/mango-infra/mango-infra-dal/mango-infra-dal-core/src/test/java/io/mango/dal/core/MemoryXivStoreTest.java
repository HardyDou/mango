package io.mango.dal.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryXivStore.
 * Pure in-memory implementation - no external dependencies.
 */
class MemoryXivStoreTest {

    private final MemoryXivStore store = new MemoryXivStore();

    @AfterEach
    void tearDown() throws Exception {
        store.close();
    }

    // ==================== put() tests ====================

    @Test
    void put_newKey_returnsTrue() {
        boolean result = store.put("k1", "v1", 3600);
        assertTrue(result);
    }

    @Test
    void put_existingKey_returnsFalse() {
        store.put("k1", "v1", 3600);
        boolean result = store.put("k1", "v2", 3600);
        assertFalse(result);
    }

    @Test
    void put_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.put(null, "v", 3600));
    }

    @Test
    void put_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.put("  ", "v", 3600));
    }

    @Test
    void put_zeroTtl_expiresImmediately() throws Exception {
        // TTL=0 should expire immediately
        store.put("k1", "v1", 0);
        String result = store.get("k1");
        assertNull(result);
    }

    // ==================== get() tests ====================

    @Test
    void get_existingKey_returnsValue() {
        store.put("k1", "v1", 3600);
        assertEquals("v1", store.get("k1"));
    }

    @Test
    void get_nonExistingKey_returnsNull() {
        assertNull(store.get("nonExisting"));
    }

    @Test
    void get_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.get(null));
    }

    @Test
    void get_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.get("  "));
    }

    // ==================== increment() tests ====================

    @Test
    void increment_firstCall_returns1() {
        long count = store.increment("counter1", 60);
        assertEquals(1, count);
    }

    @Test
    void increment_multipleCalls_increments() {
        store.increment("counter1", 60);
        store.increment("counter1", 60);
        long count = store.increment("counter1", 60);
        assertEquals(3, count);
    }

    @Test
    void increment_differentKeys_independent() {
        store.increment("counter1", 60);
        store.increment("counter1", 60);
        long count2 = store.increment("counter2", 60);
        assertEquals(1, count2);
    }

    @Test
    void increment_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.increment(null, 60));
    }

    @Test
    void increment_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.increment("  ", 60));
    }

    // ==================== delete() tests ====================

    @Test
    void delete_existingKey_removesKey() {
        store.put("k1", "v1", 3600);
        store.delete("k1");
        assertNull(store.get("k1"));
    }

    @Test
    void delete_nonExistingKey_doesNotThrow() {
        assertDoesNotThrow(() -> store.delete("nonExisting"));
    }

    @Test
    void delete_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.delete(null));
    }

    // ==================== exists() tests ====================

    @Test
    void exists_existingKey_returnsTrue() {
        store.put("k1", "v1", 3600);
        assertTrue(store.exists("k1"));
    }

    @Test
    void exists_nonExistingKey_returnsFalse() {
        assertFalse(store.exists("nonExisting"));
    }

    @Test
    void exists_expiredKey_returnsFalse() throws Exception {
        store.put("k1", "v1", 0);  // TTL=0, immediately expired
        assertFalse(store.exists("k1"));
    }

    @Test
    void exists_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.exists(null));
    }

    // ==================== AutoCloseable tests ====================

    @Test
    void close_afterClose_storeStillUsable() throws Exception {
        MemoryXivStore localStore = new MemoryXivStore();
        localStore.put("k1", "v1", 3600);
        localStore.close();

        // After close, operations should still work (cleaner thread terminates gracefully)
        assertEquals("v1", localStore.get("k1"));
    }
}
