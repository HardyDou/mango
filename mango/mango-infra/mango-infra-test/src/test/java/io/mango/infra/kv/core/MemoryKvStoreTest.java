package io.mango.infra.kv.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryKvStore.
 * Pure in-memory implementation - no external dependencies.
 */
class MemoryKvStoreTest {

    private final MemoryKvStore store = new MemoryKvStore();

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
        assertFalse(store.put("k1", "v1", 0));
        assertNull(store.get("k1"));
    }

    @Test
    void put_negativeTtl_expiresImmediately() throws Exception {
        // Negative TTL should also delete immediately
        assertFalse(store.put("k1", "v1", -1));
        assertNull(store.get("k1"));
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
        MemoryKvStore localStore = new MemoryKvStore();
        localStore.put("k1", "v1", 3600);
        localStore.close();

        // After close, operations should still work (cleaner thread terminates gracefully)
        assertEquals("v1", localStore.get("k1"));
    }

    // ==================== bucketCount constructor tests ====================

    @Test
    void constructor_customBucketCount_succeeds() {
        // 2 buckets should work fine
        MemoryKvStore store2 = new MemoryKvStore(1, 2);
        store2.put("k1", "v1", 3600);
        assertEquals("v1", store2.get("k1"));
        store2.close();
    }

    @Test
    void constructor_zeroBucketCount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryKvStore(1, 0));
    }

    @Test
    void constructor_negativeBucketCount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryKvStore(1, -1));
    }

    // ==================== concurrent increment on same key ====================

    @Test
    void increment_concurrentSameKey_correctFinalCount() throws InterruptedException {
        String key = "concurrent-counter";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> store.increment(key, 60));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        // Final count should equal number of threads
        assertEquals(threadCount, store.increment(key, 60) - 1);
    }
}
