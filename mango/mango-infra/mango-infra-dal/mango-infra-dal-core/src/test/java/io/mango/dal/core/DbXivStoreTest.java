package io.mango.dal.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DbXivStore.
 * Mocks JdbcTemplate and RedissonClient to test IKvStore contract without real DB.
 */
@ExtendWith(MockitoExtension.class)
class DbXivStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RAtomicLong atomicLong;

    private DbXivStore store;

    @BeforeEach
    void setUp() {
        store = new DbXivStore(jdbcTemplate, redissonClient);
    }

    // ==================== put() tests ====================

    @Test
    void put_insertSucceeds_returnsTrue() {
        when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(1L);
        // Mock SELECT returning empty (key doesn't exist or is expired) → DELETE + INSERT path
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);
        when(jdbcTemplate.update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class)))
                .thenReturn(1);

        boolean result = store.put("k1", "v1", 3600);

        assertTrue(result);
        verify(jdbcTemplate).update(contains("DELETE FROM"), eq("k1"));
        verify(jdbcTemplate).update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class));
    }

    @Test
    void put_updateSucceeds_returnsFalse() {
        // Mock SELECT returning existing non-expired key → UPDATE path
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of("old_value"));

        boolean result = store.put("k1", "v1", 3600);

        assertFalse(result);
        verify(jdbcTemplate).update(contains("UPDATE sys_kv_record"), eq("v1"), any(LocalDateTime.class), eq("k1"), any(LocalDateTime.class));
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
    void get_recordExists_returnsValue() {
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of("v1"));

        assertEquals("v1", store.get("k1"));
    }

    @Test
    void get_recordDoesNotExist_returnsNull() {
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("nonExisting"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        assertNull(store.get("nonExisting"));
    }

    @Test
    void get_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.get(null));
    }

    // ==================== increment() tests ====================

    @Test
    void increment_firstCall_returns1() {
        lenient().when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        lenient().when(atomicLong.incrementAndGet()).thenReturn(100L);
        // UPDATE: 3 params (expireTime, key, now) — use lenient for varargs matching
        lenient().when(jdbcTemplate.update(
                anyString(),
                any(LocalDateTime.class), eq("counter1"), any(LocalDateTime.class)))
                .thenReturn(1);
        // SELECT: 3 params (sql, RowMapper, key, now)
        lenient().when(jdbcTemplate.query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class), eq("counter1"), any(LocalDateTime.class)
        )).thenReturn(List.of("1"));

        long count = store.increment("counter1", 60);

        assertEquals(1, count);
    }

    @Test
    void increment_subsequentCall_incrementsValue() {
        lenient().when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        lenient().when(atomicLong.incrementAndGet()).thenReturn(100L);
        lenient().when(jdbcTemplate.update(
                anyString(),
                any(LocalDateTime.class), eq("counter1"), any(LocalDateTime.class)))
                .thenReturn(1);
        lenient().when(jdbcTemplate.query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class), eq("counter1"), any(LocalDateTime.class)
        )).thenReturn(List.of("5"));

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

    // ==================== delete() tests ====================

    @Test
    void delete_recordExists_deletesAndDoesNotThrow() {
        when(jdbcTemplate.update(contains("DELETE FROM sys_kv_record"), eq("k1")))
                .thenReturn(1);

        assertDoesNotThrow(() -> store.delete("k1"));
        verify(jdbcTemplate).update(contains("DELETE FROM sys_kv_record"), eq("k1"));
    }

    @Test
    void delete_nonExistingRecord_doesNotThrow() {
        when(jdbcTemplate.update(contains("DELETE FROM sys_kv_record"), eq("nonExisting")))
                .thenReturn(0);

        assertDoesNotThrow(() -> store.delete("nonExisting"));
    }

    @Test
    void delete_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.delete(null));
    }

    // ==================== exists() tests ====================

    @Test
    void exists_recordExists_returnsTrue() {
        when(jdbcTemplate.query(
                contains("SELECT COUNT(*) FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of(1));

        assertTrue(store.exists("k1"));
    }

    @Test
    void exists_recordDoesNotExist_returnsFalse() {
        when(jdbcTemplate.query(
                contains("SELECT COUNT(*) FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("nonExisting"), any(LocalDateTime.class)
        )).thenReturn(List.of(0));

        assertFalse(store.exists("nonExisting"));
    }

    @Test
    void exists_nullKey_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> store.exists(null));
    }

    // ==================== TTL edge cases ====================

    @Test
    void put_zeroTtl_insertSucceeds_returnsTrue() {
        // TTL=0 means expire_time = LocalDateTime.now(), record is already expired
        // SELECT returns empty (expired records filtered) → DELETE + INSERT path
        when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(1L);
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);
        when(jdbcTemplate.update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class)))
                .thenReturn(1);

        boolean result = store.put("k1", "v1", 0);

        assertTrue(result);
    }

    @Test
    void put_negativeTtl_insertSucceeds() {
        // Negative TTL: same as TTL=0, expired → DELETE + INSERT path
        when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(1L);
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);
        when(jdbcTemplate.update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class)))
                .thenReturn(1);

        boolean result = store.put("k1", "v1", -1);

        assertTrue(result);
    }

    @Test
    void get_expiredKey_returnsNull() {
        // Query with expire_time > NOW() filter — expired record is excluded
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());  // expired record filtered out

        assertNull(store.get("k1"));
    }

    @Test
    void exists_expiredKey_returnsFalse() {
        // COUNT with expire_time > NOW() — expired record not counted
        when(jdbcTemplate.query(
                contains("SELECT COUNT(*) FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of(0));

        assertFalse(store.exists("k1"));
    }

    // ==================== nextId() tests ====================

    @Test
    void nextId_usesRedissonAtomicLong() {
        when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(42L);
        // put() now does SELECT → DELETE + INSERT (key not found)
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);
        when(jdbcTemplate.update(contains("INSERT INTO"), eq(42L), eq("k1"), eq("v1"), any(LocalDateTime.class)))
                .thenReturn(1);

        store.put("k1", "v1", 3600);

        verify(redissonClient).getAtomicLong("kv:db:id");
        verify(atomicLong).incrementAndGet();
    }

    @Test
    void nextId_atMaxValue_resetsTo1() {
        when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(Long.MAX_VALUE);
        doNothing().when(atomicLong).set(1L);
        // put() does SELECT → DELETE + INSERT with reset id=1
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM sys_kv_record"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);
        when(jdbcTemplate.update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class)))
                .thenReturn(1);

        store.put("k1", "v1", 3600);

        verify(atomicLong).set(1L);
    }
}
