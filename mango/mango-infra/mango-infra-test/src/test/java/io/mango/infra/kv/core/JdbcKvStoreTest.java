package io.mango.infra.kv.core.jdbc;

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
 * Unit tests for JdbcKvStore.
 * Mocks JdbcTemplate and RedissonClient to test IKvStore contract without real DB.
 */
@ExtendWith(MockitoExtension.class)
class JdbcKvStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RAtomicLong atomicLong;

    private JdbcKvStore store;

    @BeforeEach
    void setUp() {
        store = new JdbcKvStore(jdbcTemplate, redissonClient);
    }

    @Test
    void constructor_withIllegalTableName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new JdbcKvStore(jdbcTemplate, redissonClient, "infra-kv-entry"));
    }

    @Test
    void constructor_withCustomTableName_usesConfiguredTableInSql() {
        JdbcKvStore customStore = new JdbcKvStore(jdbcTemplate, redissonClient, "infra_kv_custom");
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM infra_kv_custom"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of("v1"));

        assertEquals("v1", customStore.get("k1"));
    }

    // ==================== put() tests ====================

    @Test
    void put_insertSucceeds_returnsTrue() {
        when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(1L);
        // Mock SELECT returning empty (key doesn't exist or is expired) → DELETE + INSERT path
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM infra_kv_entry"),
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
    void put_existingKey_returnsFalseWithoutOverwrite() {
        // Mock SELECT returning existing non-expired key → setIfAbsent returns false
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM infra_kv_entry"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of("old_value"));

        boolean result = store.put("k1", "v1", 3600);

        assertFalse(result);
        verify(jdbcTemplate, never()).update(contains("UPDATE infra_kv_entry"), any(), any(), any(), any());
        verify(jdbcTemplate, never()).update(contains("INSERT INTO"), any(), any(), any(), any());
    }

    @Test
    void set_overwritesExistingValue() {
        when(redissonClient.getAtomicLong("kv:db:id")).thenReturn(atomicLong);
        when(atomicLong.incrementAndGet()).thenReturn(1L);
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(1);
        when(jdbcTemplate.update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class)))
                .thenReturn(1);

        store.set("k1", "v1", 3600);

        verify(jdbcTemplate).update(contains("DELETE FROM"), eq("k1"));
        verify(jdbcTemplate).update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class));
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
                contains("SELECT kv_value FROM infra_kv_entry"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of("v1"));

        assertEquals("v1", store.get("k1"));
    }

    @Test
    void get_recordDoesNotExist_returnsNull() {
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM infra_kv_entry"),
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
        // UPDATE: 4 params (delta, expireTime, key, now) — use lenient for varargs matching
        lenient().when(jdbcTemplate.update(
                anyString(),
                eq(1L), any(LocalDateTime.class), eq("counter1"), any(LocalDateTime.class)))
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
                eq(1L), any(LocalDateTime.class), eq("counter1"), any(LocalDateTime.class)))
                .thenReturn(1);
        lenient().when(jdbcTemplate.query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class), eq("counter1"), any(LocalDateTime.class)
        )).thenReturn(List.of("5"));

        long count = store.increment("counter1", 60);

        assertEquals(5, count);
    }

    @Test
    void incrementBy_withDelta_returnsUpdatedValue() {
        lenient().when(jdbcTemplate.update(
                anyString(),
                eq(5L), any(LocalDateTime.class), eq("counter1"), any(LocalDateTime.class)))
                .thenReturn(1);
        lenient().when(jdbcTemplate.query(
                anyString(),
                any(org.springframework.jdbc.core.RowMapper.class), eq("counter1"), any(LocalDateTime.class)
        )).thenReturn(List.of("5"));

        long count = store.incrementBy("counter1", 5, 60);

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
        when(jdbcTemplate.update(contains("DELETE FROM infra_kv_entry"), eq("k1")))
                .thenReturn(1);

        assertDoesNotThrow(() -> store.delete("k1"));
        verify(jdbcTemplate).update(contains("DELETE FROM infra_kv_entry"), eq("k1"));
    }

    @Test
    void delete_nonExistingRecord_doesNotThrow() {
        when(jdbcTemplate.update(contains("DELETE FROM infra_kv_entry"), eq("nonExisting")))
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
                contains("SELECT COUNT(*) FROM infra_kv_entry"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(List.of(1));

        assertTrue(store.exists("k1"));
    }

    @Test
    void exists_recordDoesNotExist_returnsFalse() {
        when(jdbcTemplate.query(
                contains("SELECT COUNT(*) FROM infra_kv_entry"),
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
    void put_zeroTtl_shouldReturnFalseAndDeleteOnly() {
        // TTL=0: delete immediately, do NOT insert
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);

        boolean result = store.put("k1", "v1", 0);

        assertFalse(result);
        verify(jdbcTemplate).update(contains("DELETE FROM"), eq("k1"));
        verify(jdbcTemplate, never()).update(contains("INSERT INTO"), any(), any(), any(), any());
    }

    @Test
    void put_negativeTtl_shouldReturnFalseAndDeleteOnly() {
        // Negative TTL: same as TTL=0, delete immediately, do NOT insert
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);

        boolean result = store.put("k1", "v1", -1);

        assertFalse(result);
        verify(jdbcTemplate).update(contains("DELETE FROM"), eq("k1"));
        verify(jdbcTemplate, never()).update(contains("INSERT INTO"), any(), any(), any(), any());
    }

    @Test
    void put_zeroTtl_existingKey_shouldReturnFalseAndDelete() {
        // TTL=0 on existing key: delete, do not re-insert
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(1);

        boolean result = store.put("k1", "v1", 0);

        assertFalse(result);
        verify(jdbcTemplate).update(contains("DELETE FROM"), eq("k1"));
        verify(jdbcTemplate, never()).update(contains("INSERT INTO"), any(), any(), any(), any());
    }

    @Test
    void get_expiredKey_returnsNull() {
        // Query with expire_time > NOW() filter — expired record is excluded
        when(jdbcTemplate.query(
                contains("SELECT kv_value FROM infra_kv_entry"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());  // expired record filtered out

        assertNull(store.get("k1"));
    }

    @Test
    void exists_expiredKey_returnsFalse() {
        // COUNT with expire_time > NOW() — expired record not counted
        when(jdbcTemplate.query(
                contains("SELECT COUNT(*) FROM infra_kv_entry"),
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
                contains("SELECT kv_value FROM infra_kv_entry"),
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
                contains("SELECT kv_value FROM infra_kv_entry"),
                any(org.springframework.jdbc.core.RowMapper.class), eq("k1"), any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());
        when(jdbcTemplate.update(contains("DELETE FROM"), eq("k1"))).thenReturn(0);
        when(jdbcTemplate.update(contains("INSERT INTO"), eq(1L), eq("k1"), eq("v1"), any(LocalDateTime.class)))
                .thenReturn(1);

        store.put("k1", "v1", 3600);

        verify(atomicLong).set(1L);
    }
}
