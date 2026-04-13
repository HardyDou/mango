package io.mango.infra.kv.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.RScript;

import java.time.Duration;
import java.util.Collections;

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

    private RedisKvStore store;

    @BeforeEach
    void setUp() {
        store = new RedisKvStore(redissonClient);
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

    @Test
    void put_zeroTtl_shouldReturnFalseAndDelete() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.delete()).thenReturn(false);

        boolean result = store.put("k1", "v1", 0);

        assertFalse(result);
        verify(bucket).delete();
    }

    @Test
    void put_negativeTtl_shouldReturnFalseAndDelete() {
        doReturn(bucket).when(redissonClient).getBucket("k1");
        when(bucket.delete()).thenReturn(false);

        boolean result = store.put("k1", "v1", -1);

        assertFalse(result);
        verify(bucket).delete();
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
    @SuppressWarnings("unchecked")
    void increment_firstCall_returns1() {
        RScript script = mock(RScript.class);
        when(redissonClient.getScript()).thenReturn(script);
        when(script.eval(any(), anyString(), any(), anyList(), any())).thenReturn(1L);

        long count = store.increment("counter1", 60);

        assertEquals(1, count);
    }

    @Test
    @SuppressWarnings("unchecked")
    void increment_subsequentCall_increments() {
        RScript script = mock(RScript.class);
        when(redissonClient.getScript()).thenReturn(script);
        when(script.eval(any(), anyString(), any(), anyList(), any())).thenReturn(5L);

        long count = store.increment("counter1", 60);

        assertEquals(5, count);
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
