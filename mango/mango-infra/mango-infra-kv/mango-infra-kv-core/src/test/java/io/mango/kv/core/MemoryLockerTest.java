package io.mango.kv.core;

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

    @Test
    void tryLock_concurrentRaceOnlyOneSucceeds() throws InterruptedException {
        String key = "race-lock";
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> results[idx] = locker.tryLock(key, 3600));
        }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        // Exactly one thread should acquire the lock
        int successCount = 0;
        for (boolean b : results) if (b) successCount++;
        assertEquals(1, successCount);
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
    void unlock_nullKey_isSilentNoOp() {
        assertDoesNotThrow(() -> locker.unlock(null));
    }

    @Test
    void unlock_blankKey_isSilentNoOp() {
        assertDoesNotThrow(() -> locker.unlock("  "));
    }

    @Test
    void unlock_wrongThread_doesNotRemoveLock() throws Exception {
        // Thread A acquires lock
        assertTrue(locker.tryLock("shared", 3600));
        // Thread B tries to unlock — should be silent no-op, not remove the lock
        Thread threadB = new Thread(() -> locker.unlock("shared"));
        threadB.start();
        threadB.join();
        // Lock still held by Thread A — Thread A cannot re-acquire
        assertFalse(locker.tryLock("shared", 3600));
        // Thread A can still unlock
        locker.unlock("shared");
        assertTrue(locker.tryLock("shared", 3600));
    }

    @Test
    void unlock_afterExpiration_acquiredByAnotherThread() throws Exception {
        // Thread A acquires lock with short TTL
        assertTrue(locker.tryLock("expiry-lock", 1));
        Thread.sleep(1100); // Wait for expiration
        // Thread B acquires the expired lock
        MemoryLocker locker2 = new MemoryLocker();
        assertTrue(locker2.tryLock("expiry-lock", 3600));
        // Thread A tries to unlock expired lock — should not remove B's lock
        locker.unlock("expiry-lock");
        // B's lock still valid — tryLock from B should fail
        assertFalse(locker2.tryLock("expiry-lock", 3600));
        locker2.unlock("expiry-lock");
    }
}