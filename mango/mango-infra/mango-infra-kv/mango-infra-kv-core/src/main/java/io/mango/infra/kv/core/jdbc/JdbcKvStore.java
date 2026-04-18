package io.mango.infra.kv.core.jdbc;

import io.mango.infra.kv.api.IKvStore;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * JdbcKvStore implementation using try-UPDATE-then-INSERT pattern.
 * Avoids ON DUPLICATE KEY UPDATE for better cross-database compatibility.
 */
@Slf4j
public class JdbcKvStore implements IKvStore {

    public static final String DEFAULT_TABLE_NAME = "infra_kv_entry";

    private static final String DEFAULT_ID_KEY = "kv:db:id";
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final JdbcTemplate jdbcTemplate;
    private final RedissonClient redissonClient;
    private final String tableName;
    private final String idKey;

    public JdbcKvStore(JdbcTemplate jdbcTemplate, RedissonClient redissonClient) {
        this(jdbcTemplate, redissonClient, DEFAULT_TABLE_NAME);
    }

    public JdbcKvStore(JdbcTemplate jdbcTemplate, RedissonClient redissonClient, String tableName) {
        this(jdbcTemplate, redissonClient, tableName, DEFAULT_ID_KEY);
    }

    public JdbcKvStore(JdbcTemplate jdbcTemplate, RedissonClient redissonClient, String tableName, String idKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.redissonClient = redissonClient;
        this.tableName = normalizeTableName(tableName);
        this.idKey = normalizeIdKey(idKey);
    }

    @Override
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            return putNonPositiveTtl(key);
        }
        // 先检查是否存在且未过期 — 过期记录视为不存在，需要删除后重新插入
        var existing = jdbcTemplate.query(
            "SELECT kv_value FROM " + tableName + " WHERE kv_key = ? AND expire_time > ?",
            (rs, rowNum) -> rs.getString("kv_value"),
            key, LocalDateTime.now());
        if (!existing.isEmpty()) {
            return false;
        }
        // key 不存在或已过期 → 删除旧记录（如有）后 INSERT
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE kv_key = ?", key);
        jdbcTemplate.update(
            "INSERT INTO " + tableName + " (id, kv_key, kv_value, expire_time) VALUES (?, ?, ?, ?)",
            nextId(), key, value, LocalDateTime.now().plusSeconds(expireSeconds));
        return true;
    }

    @Override
    public void set(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            putNonPositiveTtl(key);
            return;
        }
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE kv_key = ?", key);
        jdbcTemplate.update(
            "INSERT INTO " + tableName + " (id, kv_key, kv_value, expire_time) VALUES (?, ?, ?, ?)",
            nextId(), key, value, LocalDateTime.now().plusSeconds(expireSeconds));
    }

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        return setIfAbsent(key, value, expireSeconds);
    }

    @Override
    public String get(String key) {
        validateKey(key);
        var results = jdbcTemplate.query(
            "SELECT kv_value FROM " + tableName + " WHERE kv_key = ? AND expire_time > ?",
            (rs, rowNum) -> rs.getString("kv_value"),
            key, LocalDateTime.now());
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public long incrementBy(String key, long delta, long windowSeconds) {
        validateKey(key);
        if (windowSeconds <= 0) {
            throw new IllegalArgumentException("windowSeconds must be positive, was: " + windowSeconds);
        }
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(windowSeconds);
        // 先尝试 UPDATE（原子操作），失败才 INSERT，兼容 H2/MySQL 类型转换差异
        int updated = jdbcTemplate.update(
            "UPDATE " + tableName + " SET kv_value = CAST(kv_value AS SIGNED) + ?, expire_time = ?"
                + " WHERE kv_key = ? AND expire_time > ?",
            delta, expireTime, key, LocalDateTime.now());
        if (updated == 0) {
            // 不存在或已过期 → INSERT
            jdbcTemplate.update("DELETE FROM " + tableName + " WHERE kv_key = ?", key);
            jdbcTemplate.update(
                "INSERT INTO " + tableName + " (id, kv_key, kv_value, expire_time) VALUES (?, ?, ?, ?)",
                nextId(), key, String.valueOf(delta), expireTime);
        }
        var results = jdbcTemplate.query(
            "SELECT kv_value FROM " + tableName + " WHERE kv_key = ? AND expire_time > ? ORDER BY create_time DESC LIMIT 1",
            (rs, rowNum) -> rs.getString("kv_value"),
            key, LocalDateTime.now());
        return results.isEmpty() ? 0 : Long.parseLong(results.get(0));
    }

    @Override
    public long increment(String key, long windowSeconds) {
        return incrementBy(key, 1, windowSeconds);
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE kv_key = ?", key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        var results = jdbcTemplate.query(
            "SELECT COUNT(*) FROM " + tableName + " WHERE kv_key = ? AND expire_time > ?",
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
        RAtomicLong atomicLong = redissonClient.getAtomicLong(idKey);
        long id = atomicLong.incrementAndGet();
        if (id == Long.MAX_VALUE) {
            atomicLong.set(1);
            return 1;
        }
        return id;
    }

    private String normalizeIdKey(String configuredIdKey) {
        if (configuredIdKey == null || configuredIdKey.trim().isEmpty()) {
            throw new IllegalArgumentException("idKey cannot be null or blank");
        }
        return configuredIdKey.trim();
    }

    private String normalizeTableName(String configuredTableName) {
        String candidate = configuredTableName == null || configuredTableName.isBlank()
                ? DEFAULT_TABLE_NAME
                : configuredTableName.trim();
        if (!TABLE_NAME_PATTERN.matcher(candidate).matches()) {
            throw new IllegalArgumentException("tableName must match [A-Za-z_][A-Za-z0-9_]*");
        }
        return candidate;
    }
}
