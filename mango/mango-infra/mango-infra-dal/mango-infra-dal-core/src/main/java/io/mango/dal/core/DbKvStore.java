package io.mango.dal.core;

import io.mango.dal.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/**
 * DbKvStore implementation using try-UPDATE-then-INSERT pattern.
 * Avoids ON DUPLICATE KEY UPDATE for better cross-database compatibility.
 */
@Slf4j
@RequiredArgsConstructor
public class DbKvStore implements IKvStore {

    private static final String ID_KEY = "kv:db:id";

    private final JdbcTemplate jdbcTemplate;
    private final RedissonClient redissonClient;

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            // TTL=0 或负数：立即删除，不存储
            jdbcTemplate.update("DELETE FROM sys_kv_record WHERE kv_key = ?", key);
            return false;
        }
        // 先检查是否存在且未过期 — 过期记录视为不存在，需要删除后重新插入
        var existing = jdbcTemplate.query(
            "SELECT kv_value FROM sys_kv_record WHERE kv_key = ? AND expire_time > ?",
            (rs, rowNum) -> rs.getString("kv_value"),
            key, LocalDateTime.now());
        if (!existing.isEmpty()) {
            // key 存在且未过期 → UPDATE
            jdbcTemplate.update("""
                UPDATE sys_kv_record SET kv_value = ?, expire_time = ? WHERE kv_key = ? AND expire_time > ?
                """, value, LocalDateTime.now().plusSeconds(expireSeconds), key, LocalDateTime.now());
            return false;
        }
        // key 不存在或已过期 → 删除旧记录（如有）后 INSERT
        jdbcTemplate.update("DELETE FROM sys_kv_record WHERE kv_key = ?", key);
        jdbcTemplate.update("""
            INSERT INTO sys_kv_record (id, kv_key, kv_value, expire_time)
            VALUES (?, ?, ?, ?)
            """, nextId(), key, value, LocalDateTime.now().plusSeconds(expireSeconds));
        return true;
    }

    @Override
    public String get(String key) {
        validateKey(key);
        var results = jdbcTemplate.query(
            "SELECT kv_value FROM sys_kv_record WHERE kv_key = ? AND expire_time > ?",
            (rs, rowNum) -> rs.getString("kv_value"),
            key, LocalDateTime.now());
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public long increment(String key, long windowSeconds) {
        validateKey(key);
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(windowSeconds);
        // 先尝试 UPDATE（原子操作），失败才 INSERT，兼容 H2/MySQL 类型转换差异
        int updated = jdbcTemplate.update("""
            UPDATE sys_kv_record SET kv_value = CAST(kv_value AS SIGNED) + 1, expire_time = ?
            WHERE kv_key = ? AND expire_time > ?
            """, expireTime, key, LocalDateTime.now());
        if (updated == 0) {
            // 不存在或已过期 → INSERT
            jdbcTemplate.update("""
                INSERT INTO sys_kv_record (id, kv_key, kv_value, expire_time)
                VALUES (?, ?, '1', ?)
                """, nextId(), key, expireTime);
        }
        var results = jdbcTemplate.query(
            "SELECT kv_value FROM sys_kv_record WHERE kv_key = ? AND expire_time > ? ORDER BY create_time DESC LIMIT 1",
            (rs, rowNum) -> rs.getString("kv_value"),
            key, LocalDateTime.now());
        return results.isEmpty() ? 0 : Long.parseLong(results.get(0));
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        jdbcTemplate.update("DELETE FROM sys_kv_record WHERE kv_key = ?", key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        var results = jdbcTemplate.query(
            "SELECT COUNT(*) FROM sys_kv_record WHERE kv_key = ? AND expire_time > ?",
            (rs, rowNum) -> rs.getInt(1),
            key, LocalDateTime.now());
        return !results.isEmpty() && results.get(0) > 0;
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private long nextId() {
        RAtomicLong atomicLong = redissonClient.getAtomicLong(ID_KEY);
        long id = atomicLong.incrementAndGet();
        if (id == Long.MAX_VALUE) {
            atomicLong.set(1);
            return 1;
        }
        return id;
    }
}
