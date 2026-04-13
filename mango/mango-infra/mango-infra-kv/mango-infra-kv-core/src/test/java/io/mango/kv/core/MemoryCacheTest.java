package io.mango.kv.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryCache.
 */
class MemoryCacheTest {

    private final MemoryCache cache = new MemoryCache();

    @AfterEach
    void tearDown() throws Exception {
        cache.close();
    }

    // ==================== set() tests ====================

    @Test
    void set_newKey_valueStored() {
        cache.set("k1", "v1", 3600);
        assertEquals("v1", cache.get("k1"));
    }

    @Test
    void set_existingKey_valueOverwritten() {
        cache.set("k1", "v1", 3600);
        cache.set("k1", "v2", 3600);
        assertEquals("v2", cache.get("k1"));
    }

    @Test
    void set_negativeTtl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.set("k1", "v1", -1));
    }

    @Test
    void set_zeroTtl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.set("k1", "v1", 0));
    }

    @Test
    void set_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.set(null, "v1", 3600));
    }

    @Test
    void set_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.set("  ", "v1", 3600));
    }

    @Test
    void set_nullValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.set("k1", null, 3600));
    }

    // ==================== get() tests ====================

    @Test
    void get_existingKey_returnsValue() {
        cache.set("k1", "v1", 3600);
        assertEquals("v1", cache.get("k1"));
    }

    @Test
    void get_nonExistingKey_returnsNull() {
        assertNull(cache.get("nonExisting"));
    }

    @Test
    void get_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.get(null));
    }

    @Test
    void get_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.get("  "));
    }

    // ==================== exists() tests ====================

    @Test
    void exists_existingKey_returnsTrue() {
        cache.set("k1", "v1", 3600);
        assertTrue(cache.exists("k1"));
    }

    @Test
    void exists_nonExistingKey_returnsFalse() {
        assertFalse(cache.exists("nonExisting"));
    }

    @Test
    void exists_expiredKey_returnsFalse() throws InterruptedException {
        cache.set("k1", "v1", 1);
        Thread.sleep(1100);  // Wait for expiration
        assertFalse(cache.exists("k1"));
    }

    @Test
    void exists_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.exists(null));
    }

    // ==================== delete() tests ====================

    @Test
    void delete_existingKey_removesKey() {
        cache.set("k1", "v1", 3600);
        cache.delete("k1");
        assertNull(cache.get("k1"));
    }

    @Test
    void delete_nonExistingKey_doesNotThrow() {
        assertDoesNotThrow(() -> cache.delete("nonExisting"));
    }

    @Test
    void delete_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> cache.delete(null));
    }

    // ==================== AutoCloseable tests ====================

    @Test
    void close_afterClose_stillUsable() throws Exception {
        MemoryCache localCache = new MemoryCache();
        localCache.set("k1", "v1", 3600);
        localCache.close();
        assertEquals("v1", localCache.get("k1"));
    }
}