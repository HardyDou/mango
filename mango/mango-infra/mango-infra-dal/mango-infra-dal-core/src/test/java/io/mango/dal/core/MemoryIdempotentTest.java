package io.mango.dal.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryIdempotent.
 */
class MemoryIdempotentTest {

    private final MemoryIdempotent idempotent = new MemoryIdempotent();

    // ==================== isDuplicate() tests ====================

    @Test
    void isDuplicate_newKey_returnsFalse() {
        assertFalse(idempotent.isDuplicate("key1", 3600));
    }

    @Test
    void isDuplicate_afterMark_returnsTrue() {
        idempotent.mark("key1", 3600);
        assertTrue(idempotent.isDuplicate("key1", 3600));
    }

    @Test
    void isDuplicate_expiredKey_returnsFalse() throws InterruptedException {
        idempotent.mark("key1", 1);
        Thread.sleep(1100);
        assertFalse(idempotent.isDuplicate("key1", 3600));
    }

    @Test
    void isDuplicate_differentKeys_independent() {
        idempotent.mark("key1", 3600);
        assertFalse(idempotent.isDuplicate("key2", 3600));
    }

    @Test
    void isDuplicate_zeroWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.isDuplicate("key1", 0));
    }

    @Test
    void isDuplicate_negativeWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.isDuplicate("key1", -1));
    }

    @Test
    void isDuplicate_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.isDuplicate(null, 3600));
    }

    @Test
    void isDuplicate_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.isDuplicate("  ", 3600));
    }

    // ==================== mark() tests ====================

    @Test
    void mark_withPositiveWindow_succeeds() {
        idempotent.mark("key1", 3600);
        assertTrue(idempotent.isDuplicate("key1", 3600));
    }

    @Test
    void mark_zeroWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.mark("key1", 0));
    }

    @Test
    void mark_negativeWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.mark("key1", -1));
    }

    @Test
    void mark_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.mark(null, 3600));
    }

    @Test
    void mark_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.mark("  ", 3600));
    }
}