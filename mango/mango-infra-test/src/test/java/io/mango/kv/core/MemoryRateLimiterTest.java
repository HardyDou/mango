package io.mango.kv.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryRateLimiter.
 */
class MemoryRateLimiterTest {

    private final MemoryRateLimiter rateLimiter = new MemoryRateLimiter();

    // ==================== tryAcquire() tests ====================

    @Test
    void tryAcquire_singlePermit_firstCall_returnsTrue() {
        assertTrue(rateLimiter.tryAcquire("limiter1", 1));
    }

    @Test
    void tryAcquire_multiplePermitsWithinLimit_returnsTrue() {
        assertTrue(rateLimiter.tryAcquire("limiter1", 50));
        assertTrue(rateLimiter.tryAcquire("limiter1", 50));
    }

    @Test
    void tryAcquire_exceedsLimit_returnsFalse() {
        // Exhaust all tokens
        for (int i = 0; i < 100; i++) {
            rateLimiter.tryAcquire("limiter1", 1);
        }
        assertFalse(rateLimiter.tryAcquire("limiter1", 1));
    }

    @Test
    void tryAcquire_differentKeys_independent() {
        // Exhaust limiter1
        for (int i = 0; i < 100; i++) {
            rateLimiter.tryAcquire("limiter1", 1);
        }
        // limiter2 should still work
        assertTrue(rateLimiter.tryAcquire("limiter2", 1));
    }

    @Test
    void tryAcquire_zeroPermits_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.tryAcquire("limiter1", 0));
    }

    @Test
    void tryAcquire_negativePermits_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.tryAcquire("limiter1", -1));
    }

    @Test
    void tryAcquire_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.tryAcquire(null, 1));
    }

    @Test
    void tryAcquire_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.tryAcquire("  ", 1));
    }

    @Test
    void tryAcquire_afterRefill_returnsTrueAgain() throws InterruptedException {
        // Exhaust tokens
        for (int i = 0; i < 100; i++) {
            rateLimiter.tryAcquire("limiter1", 1);
        }
        assertFalse(rateLimiter.tryAcquire("limiter1", 1));
        // Wait for refill (1 token per second)
        Thread.sleep(1100);
        assertTrue(rateLimiter.tryAcquire("limiter1", 1));
    }
}