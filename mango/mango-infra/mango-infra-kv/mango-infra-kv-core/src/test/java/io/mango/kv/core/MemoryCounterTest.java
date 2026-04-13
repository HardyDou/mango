package io.mango.kv.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryCounter.
 */
class MemoryCounterTest {

    private final MemoryCounter counter = new MemoryCounter();

    // ==================== increment() tests ====================

    @Test
    void increment_firstCall_withPositiveDelta_returnsDelta() {
        assertEquals(5, counter.increment("counter1", 5, 3600));
    }

    @Test
    void increment_multipleCalls_accumulates() {
        counter.increment("counter1", 1, 3600);
        counter.increment("counter1", 2, 3600);
        // After increment(1) -> 1, increment(2) -> 3, increment(1) -> 4
        assertEquals(4, counter.increment("counter1", 1, 3600));
    }

    @Test
    void increment_negativeDelta_decrements() {
        counter.increment("counter1", 10, 3600);
        assertEquals(5, counter.increment("counter1", -5, 3600));
    }

    @Test
    void increment_differentKeys_independent() {
        counter.increment("counter1", 5, 3600);
        counter.increment("counter2", 10, 3600);
        assertEquals(5, counter.get("counter1"));
        assertEquals(10, counter.get("counter2"));
    }

    @Test
    void increment_expiredKey_startsFresh() throws InterruptedException {
        counter.increment("counter1", 10, 1);
        Thread.sleep(1100);
        assertEquals(5, counter.increment("counter1", 5, 3600));
    }

    @Test
    void increment_zeroWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> counter.increment("counter1", 1, 0));
    }

    @Test
    void increment_negativeWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> counter.increment("counter1", 1, -1));
    }

    @Test
    void increment_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> counter.increment(null, 1, 3600));
    }

    @Test
    void increment_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> counter.increment("  ", 1, 3600));
    }

    // ==================== get() tests ====================

    @Test
    void get_existingKey_returnsValue() {
        counter.increment("counter1", 5, 3600);
        assertEquals(5, counter.get("counter1"));
    }

    @Test
    void get_nonExistingKey_returnsZero() {
        assertEquals(0, counter.get("nonExisting"));
    }

    @Test
    void get_expiredKey_returnsZero() throws InterruptedException {
        counter.increment("counter1", 5, 1);
        Thread.sleep(1100);
        assertEquals(0, counter.get("counter1"));
    }

    @Test
    void get_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> counter.get(null));
    }

    @Test
    void get_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> counter.get("  "));
    }
}