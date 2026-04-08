package io.mango.dal.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryLocker.
 */
class MemoryLockerTest {

    private final MemoryLocker locker = new MemoryLocker();

    // ==================== tryLock() tests ====================

    @Test
    void tryLock_newKey_returnsTrue() {
        assertTrue(locker.tryLock("lock1", 3600));
    }

    @Test
    void tryLock_sameKeyTwice_returnsFalse() {
        locker.tryLock("lock1", 3600);
        assertFalse(locker.tryLock("lock1", 3600));
    }

    @Test
    void tryLock_differentKeys_independent() {
        locker.tryLock("lock1", 3600);
        assertTrue(locker.tryLock("lock2", 3600));
    }

    @Test
    void tryLock_expiredLock_canAcquireAgain() throws InterruptedException {
        locker.tryLock("lock1", 1);
        Thread.sleep(1100);  // Wait for expiration
        assertTrue(locker.tryLock("lock1", 3600));
    }

    @Test
    void tryLock_zeroTtl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> locker.tryLock("lock1", 0));
    }

    @Test
    void tryLock_negativeTtl_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> locker.tryLock("lock1", -1));
    }

    @Test
    void tryLock_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> locker.tryLock(null, 3600));
    }

    @Test
    void tryLock_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> locker.tryLock("  ", 3600));
    }

    // ==================== unlock() tests ====================

    @Test
    void unlock_afterLock_canAcquireAgain() {
        locker.tryLock("lock1", 3600);
        locker.unlock("lock1");
        assertTrue(locker.tryLock("lock1", 3600));
    }

    @Test
    void unlock_nonExistingLock_doesNotThrow() {
        assertDoesNotThrow(() -> locker.unlock("nonExisting"));
    }

    @Test
    void unlock_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> locker.unlock(null));
    }

    @Test
    void unlock_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> locker.unlock("  "));
    }
}