package io.mango.infra.kv.core.memory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryKvStore specific tests.
 * Contract behavior lives in KvStoreContractTest.
 */
class MemoryKvStoreTest {

    private final MemoryKvStore store = new MemoryKvStore();

    @AfterEach
    void tearDown() throws Exception {
        store.close();
    }

    @Test
    void close_afterClose_storeStillUsable() throws Exception {
        MemoryKvStore localStore = new MemoryKvStore();
        localStore.put("k1", "v1", 3600);
        localStore.close();

        // After close, operations should still work (cleaner thread terminates gracefully)
        assertEquals("v1", localStore.get("k1"));
    }

    @Test
    void constructor_customBucketCount_succeeds() {
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
