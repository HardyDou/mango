package io.mango.dal.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisXivStore.
 * Mocks RedissonClient to test IKvStore contract without real Redis.
 */
@ExtendWith(MockitoExtension.class)
class RedisXivStoreTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> bucket;

    @Mock
    private RAtomicLong atomicLong;

    private RedisXivStore store;

    @BeforeEach
    void setUp() {
        store = new RedisXivStore(redissonClient);
    }

    // ==================== put() tests ====================

    @Test
    void put_setIfAbsentSucceeds_returnsTrue() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.setIfAbsent(eq("v1"), eq(Duration.ofSeconds(3600)))).thenReturn(true);

        boolean result = store.put("k1", "v1", 3600);

        assertTrue(result);
        verify(bucket).setIfAbsent("v1", Duration.ofSeconds(3600));
    }

    @Test
    void put_keyAlreadyExists_returnsFalse() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.setIfAbsent(eq("v1"), eq(Duration.ofSeconds(3600)))).thenReturn(false);

        boolean result = store.put("k1", "v1", 3600);

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

    // ==================== get() tests ====================

    @Test
    void get_bucketHasValue_returnsValue() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.get()).thenReturn("v1");

        assertEquals("v1", store.get("k1"));
    }

    @Test
    void get_bucketIsNull_returnsNull() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.get()).thenReturn(null);

        assertNull(store.get("k1"));
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
    void increment_firstCall_returns1AndSetsExpiry() {
        when(redissonClient.getAtomicLong("counter1")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(1L);
        when(atomicLong.expire(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);

        long count = store.increment("counter1", 60);

        assertEquals(1, count);
        verify(atomicLong).expire(60, TimeUnit.SECONDS);
    }

    @Test
    void increment_subsequentCalls_incrementsWithoutResettingExpiry() {
        when(redissonClient.getAtomicLong("counter1")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(2L);

        long count = store.increment("counter1", 60);

        assertEquals(2, count);
        // expire should NOT be called when count > 1
        verify(atomicLong, never()).expire(anyLong(), any(TimeUnit.class));
    }

    @Test
    void increment_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.increment(null, 60));
    }

    // ==================== delete() tests ====================

    @Test
    void delete_bucketExists_deletes() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.delete()).thenReturn(true);

        store.delete("k1");

        verify(bucket).delete();
    }

    @Test
    void delete_nonExistingKey_doesNotThrow() {
        doReturn(bucket).when(redissonClient).getBucket("nonExisting");
        when(bucket.delete()).thenReturn(false);

        assertDoesNotThrow(() -> store.delete("nonExisting"));
    }

    @Test
    void delete_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.delete(null));
    }

    // ==================== exists() tests ====================

    @Test
    void exists_bucketExists_returnsTrue() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.isExists()).thenReturn(true);

        assertTrue(store.exists("k1"));
    }

    @Test
    void exists_bucketDoesNotExist_returnsFalse() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.isExists()).thenReturn(false);

        assertFalse(store.exists("k1"));
    }

    @Test
    void exists_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.exists(null));
    }

    // ==================== TTL edge cases ====================

    @Test
    void put_zeroTtl_setIfAbsentReturnsFalse_butValueStillStored() {
        // Redis setIfAbsent with Duration.ofSeconds(0) stores value without expiry
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.setIfAbsent(eq("v1"), eq(Duration.ofSeconds(0)))).thenReturn(false);

        boolean result = store.put("k1", "v1", 0);

        assertFalse(result);
        verify(bucket).setIfAbsent("v1", Duration.ofSeconds(0));
    }

    @Test
    void put_negativeTtl_stillStores() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.setIfAbsent(eq("v1"), eq(Duration.ofSeconds(-1)))).thenReturn(true);

        boolean result = store.put("k1", "v1", -1);

        assertTrue(result);
    }

    @Test
    void exists_expiredKey_returnsFalse() {
        // Redis natively handles expiry — isExists returns false for expired keys
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.isExists()).thenReturn(false);

        assertFalse(store.exists("k1"));
    }

    @Test
    void exists_nonExpiredKey_returnsTrue() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.isExists()).thenReturn(true);

        assertTrue(store.exists("k1"));
    }
}
