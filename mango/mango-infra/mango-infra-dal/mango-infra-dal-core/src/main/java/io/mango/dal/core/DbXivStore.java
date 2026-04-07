package io.mango.dal.core;

import io.mango.dal.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/**
 * DbXivStore implementation using INSERT ON DUPLICATE KEY UPDATE.
 */
@Slf4j
@RequiredArgsConstructor
public class DbXivStore implements IKvStore {

    private final JdbcTemplate jdbcTemplate;
    private final RedissonClient redissonClient;

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
        // Use Redisson AtomicLong for collision-free unique ID
        RAtomicLong atomicLong = redissonClient.getAtomicLong("kv:db:id");
        long id = atomicLong.incrementAndGet();
        if (id == Long.MAX_VALUE) {
            // Reset and start over (extremely unlikely)
            atomicLong.set(1);
            return 1;
        }
        return id;
    }
}
