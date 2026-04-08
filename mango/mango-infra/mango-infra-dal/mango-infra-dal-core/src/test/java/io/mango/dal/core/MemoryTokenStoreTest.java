package io.mango.dal.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryTokenStore.
 */
class MemoryTokenStoreTest {

    private final MemoryTokenStore tokenStore = new MemoryTokenStore();

    @AfterEach
    void tearDown() throws Exception {
        tokenStore.close();
    }

    // ==================== store() tests ====================

    @Test
    void store_newToken_valueStored() {
        tokenStore.store("token1", "user123", 3600);
        assertEquals("user123", tokenStore.get("token1"));
    }

    @Test
    void store_existingToken_valueOverwritten() {
        tokenStore.store("token1", "user123", 3600);
        tokenStore.store("token1", "user456", 3600);
        assertEquals("user456", tokenStore.get("token1"));
    }

    @Test
    void store_negativeTtl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.store("token1", "user123", -1));
    }

    @Test
    void store_zeroTtl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.store("token1", "user123", 0));
    }

    @Test
    void store_nullToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.store(null, "user123", 3600));
    }

    @Test
    void store_blankToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.store("  ", "user123", 3600));
    }

    @Test
    void store_nullValue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.store("token1", null, 3600));
    }

    // ==================== get() tests ====================

    @Test
    void get_existingToken_returnsValue() {
        tokenStore.store("token1", "user123", 3600);
        assertEquals("user123", tokenStore.get("token1"));
    }

    @Test
    void get_nonExistingToken_returnsNull() {
        assertNull(tokenStore.get("nonExisting"));
    }

    @Test
    void get_expiredToken_returnsNull() throws InterruptedException {
        tokenStore.store("token1", "user123", 1);
        Thread.sleep(1100);
        assertNull(tokenStore.get("token1"));
    }

    @Test
    void get_nullToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.get(null));
    }

    @Test
    void get_blankToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.get("  "));
    }

    // ==================== remove() tests ====================

    @Test
    void remove_existingToken_removesToken() {
        tokenStore.store("token1", "user123", 3600);
        tokenStore.remove("token1");
        assertNull(tokenStore.get("token1"));
    }

    @Test
    void remove_nonExistingToken_doesNotThrow() {
        assertDoesNotThrow(() -> tokenStore.remove("nonExisting"));
    }

    @Test
    void remove_nullToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.remove(null));
    }

    @Test
    void remove_blankToken_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> tokenStore.remove("  "));
    }

    // ==================== AutoCloseable tests ====================

    @Test
    void close_afterClose_stillUsable() throws Exception {
        MemoryTokenStore localStore = new MemoryTokenStore();
        localStore.store("token1", "user123", 3600);
        localStore.close();
        assertEquals("user123", localStore.get("token1"));
    }
}