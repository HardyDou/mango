package io.mango.kv.db;

import io.mango.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/**
 * Database KV store implementation using INSERT ON DUPLICATE KEY UPDATE.
 */
@Slf4j
@RequiredArgsConstructor
public class DbKvStore implements IKvStore {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        validateKey(key);
        int rows = jdbcTemplate.update("""
            INSERT INTO sys_kv_record (id, kv_key, kv_value, expire_time)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE kv_value = VALUES(kv_value), expire_time = VALUES(expire_time)
            """, nextId(), key, value, LocalDateTime.now().plusSeconds(expireSeconds));
        return rows == 1;
    }

    @Override
    public String get(String key) {
        validateKey(key);
        return jdbcTemplate.queryForObject(
            "SELECT kv_value FROM sys_kv_record WHERE kv_key = ? AND expire_time > ?",
            String.class, key, LocalDateTime.now());
    }

    @Override
    public long increment(String key, long windowSeconds) {
        validateKey(key);
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(windowSeconds);
        jdbcTemplate.update("""
            INSERT INTO sys_kv_record (id, kv_key, kv_value, expire_time)
            VALUES (?, ?, '1', ?)
            ON DUPLICATE KEY UPDATE kv_value = kv_value + 1
            """, nextId(), key, expireTime);
        String count = jdbcTemplate.queryForObject(
            "SELECT kv_value FROM sys_kv_record WHERE kv_key = ? AND expire_time > ? ORDER BY create_time DESC LIMIT 1",
            String.class, key, LocalDateTime.now());
        return count != null ? Long.parseLong(count) : 0;
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        jdbcTemplate.update("DELETE FROM sys_kv_record WHERE kv_key = ?", key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_kv_record WHERE kv_key = ? AND expire_time > ?",
            Integer.class, key, LocalDateTime.now());
        return count != null && count > 0;
    }

    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or blank");
        }
    }

    private long nextId() {
        // Use Redis INCR for collision-free unique ID
        Long id = redisTemplate.opsForValue().increment("kv:db:id", 1);
        return id != null ? id : fallbackNextId();
    }

    private long fallbackNextId() {
        // Fallback using timestamp + random (still has collision risk but better than original)
        return System.currentTimeMillis() * 1000 + (long) (Math.random() * 1000);
    }
}
