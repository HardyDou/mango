package io.mango.infra.kv.core.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisKvStore.
 * Mocks RedissonClient to test IKvStore contract without real Redis.
 */
@ExtendWith(MockitoExtension.class)
class RedisKvStoreTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> bucket;

    @Mock
    private RAtomicLong atomicLong;

    @Mock
    private RKeys keys;

    private RedisKvStore store;

    @BeforeEach
    void setUp() {
        store = new RedisKvStore(redissonClient);
        lenient().doReturn(bucket).when(redissonClient).getBucket(anyString(), any(Codec.class));
        lenient().doReturn(atomicLong).when(redissonClient).getAtomicLong(anyString());
        lenient().when(redissonClient.getKeys()).thenReturn(keys);
    }

    // ==================== put() tests ====================

    @Test
    void put_setIfAbsentSucceeds_returnsTrue() {
        when(bucket.setIfAbsent(eq("v1"), eq(Duration.ofSeconds(3600)))).thenReturn(true);

        boolean result = store.put("k1", "v1", 3600);

        assertTrue(result);
        verify(bucket).setIfAbsent("v1", Duration.ofSeconds(3600));
    }

    @Test
    void put_keyAlreadyExists_returnsFalse() {
        when(bucket.setIfAbsent(eq("v1"), eq(Duration.ofSeconds(3600)))).thenReturn(false);

        boolean result = store.put("k1", "v1", 3600);

        assertFalse(result);
    }

    @Test
    void set_overwritesValue() {
        store.set("k1", "v2", 3600);

        verify(bucket).set("v2", Duration.ofSeconds(3600));
    }

    @Test
    void put_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.put(null, "v", 3600));
    }

    @Test
    void put_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.put("  ", "v", 3600));
    }

    @Test
    void put_zeroTtl_shouldReturnFalseAndDelete() {
        when(keys.delete("k1")).thenReturn(0L);

        boolean result = store.put("k1", "v1", 0);

        assertFalse(result);
        verify(keys).delete("k1");
    }

    @Test
    void put_negativeTtl_shouldReturnFalseAndDelete() {
        when(keys.delete("k1")).thenReturn(0L);

        boolean result = store.put("k1", "v1", -1);

        assertFalse(result);
        verify(keys).delete("k1");
    }

    // ==================== get() tests ====================

    @Test
    void get_bucketHasValue_returnsValue() {
        when(bucket.get()).thenReturn("v1");

        assertEquals("v1", store.get("k1"));
    }

    @Test
    void get_bucketIsNull_returnsNull() {
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
    void increment_firstCall_returns1() {
        when(atomicLong.addAndGet(1L)).thenReturn(1L);

        long count = store.increment("counter1", 60);

        assertEquals(1, count);
        verify(atomicLong).expire(Duration.ofSeconds(60));
    }

    @Test
    void increment_subsequentCall_increments() {
        when(atomicLong.addAndGet(1L)).thenReturn(5L);

        long count = store.increment("counter1", 60);

        assertEquals(5, count);
        verify(atomicLong, never()).expire(any(Duration.class));
    }

    @Test
    void incrementBy_withDelta_addsDelta() {
        when(atomicLong.addAndGet(5L)).thenReturn(5L);

        long count = store.incrementBy("counter1", 5, 60);

        assertEquals(5, count);
        verify(atomicLong).expire(Duration.ofSeconds(60));
    }

    @Test
    void increment_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.increment(null, 60));
    }

    @Test
    void increment_blankKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.increment("  ", 60));
    }

    @Test
    void increment_zeroWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.increment("counter", 0));
    }

    @Test
    void increment_negativeWindow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.increment("counter", -1));
    }

    // ==================== delete() tests ====================

    @Test
    void delete_bucketExists_deletes() {
        when(keys.delete("k1")).thenReturn(1L);

        store.delete("k1");

        verify(keys).delete("k1");
    }

    @Test
    void delete_nonExistingKey_doesNotThrow() {
        when(keys.delete("nonExisting")).thenReturn(0L);

        assertDoesNotThrow(() -> store.delete("nonExisting"));
    }

    @Test
    void delete_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.delete(null));
    }

    // ==================== exists() tests ====================

    @Test
    void exists_bucketExists_returnsTrue() {
        when(keys.countExists("k1")).thenReturn(1L);

        assertTrue(store.exists("k1"));
    }

    @Test
    void exists_bucketDoesNotExist_returnsFalse() {
        when(keys.countExists("k1")).thenReturn(0L);

        assertFalse(store.exists("k1"));
    }

    @Test
    void exists_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.exists(null));
    }

    @Test
    void exists_expiredKey_returnsFalse() {
        // Redis natively handles expiry; countExists returns 0 for expired keys.
        when(keys.countExists("k1")).thenReturn(0L);

        assertFalse(store.exists("k1"));
    }

    @Test
    void exists_nonExpiredKey_returnsTrue() {
        when(keys.countExists("k1")).thenReturn(1L);

        assertTrue(store.exists("k1"));
    }
}
