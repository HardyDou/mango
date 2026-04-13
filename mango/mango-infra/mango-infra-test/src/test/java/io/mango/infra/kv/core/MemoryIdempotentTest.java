package io.mango.infra.kv.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryIdempotent.
 */
class MemoryIdempotentTest {

    private final MemoryIdempotent idempotent = new MemoryIdempotent();

    @AfterEach
    void tearDown() throws Exception {
        idempotent.close();
    }

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

    // ==================== checkAndMark() tests ====================

    @Test
    void checkAndMark_newKey_returnsFalseAndMarks() {
        assertFalse(idempotent.checkAndMark("key1", 3600));
        assertTrue(idempotent.isDuplicate("key1", 3600)); // now marked
    }

    @Test
    void checkAndMark_alreadyMarked_returnsTrue() {
        idempotent.mark("key1", 3600);
        assertTrue(idempotent.checkAndMark("key1", 3600));
    }

    @Test
    void checkAndMark_expiredKey_returnsFalseAndReMarks() throws InterruptedException {
        idempotent.mark("key1", 1);
        Thread.sleep(1100);
        assertFalse(idempotent.checkAndMark("key1", 3600)); // expired, re-marked
        assertTrue(idempotent.isDuplicate("key1", 3600));
    }

    @Test
    void checkAndMark_differentKeys_independent() {
        idempotent.checkAndMark("key1", 3600);
        assertFalse(idempotent.checkAndMark("key2", 3600));
    }

    @Test
    void checkAndMark_concurrentRace_bothReturnOneTrueOneFalse() throws InterruptedException {
        // Simulate race: two threads, one should win
        String key = "racekey";
        try (MemoryIdempotent shared = new MemoryIdempotent()) {
            boolean[] results = new boolean[2];
            Thread[] threads = new Thread[2];
            for (int i = 0; i < 2; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    results[idx] = shared.checkAndMark(key, 3600);
                });
            }
            for (Thread t : threads) t.start();
            for (Thread t : threads) t.join();
            // One got false (new), one got true (duplicate)
            int trueCount = 0, falseCount = 0;
            for (boolean b : results) {
                if (b) trueCount++;
                else falseCount++;
            }
            assertEquals(1, trueCount);
            assertEquals(1, falseCount);
        }
    }

    @Test
    void checkAndMark_zeroWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.checkAndMark("key1", 0));
    }

    @Test
    void checkAndMark_negativeWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.checkAndMark("key1", -1));
    }

    @Test
    void checkAndMark_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.checkAndMark(null, 3600));
    }

    @Test
    void checkAndMark_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> idempotent.checkAndMark("  ", 3600));
    }

    // ==================== cleanup / AutoCloseable tests ====================

    @Test
    void constructor_zeroCleanupInterval_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryIdempotent(0));
    }

    @Test
    void constructor_negativeCleanupInterval_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MemoryIdempotent(-1));
    }

    @Test
    void close_afterClose_storeStillUsable() {
        MemoryIdempotent local = new MemoryIdempotent();
        local.mark("key1", 3600);
        local.close();
        // After close, operations should still work (cleaner thread terminates gracefully)
        assertTrue(local.isDuplicate("key1", 3600));
    }
}
