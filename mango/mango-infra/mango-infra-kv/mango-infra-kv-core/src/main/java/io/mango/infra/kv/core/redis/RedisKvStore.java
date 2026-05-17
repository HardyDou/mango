package io.mango.infra.kv.core.redis;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * RedisKvStore implementation using Redisson.
 *
 * Uses RAtomicLong for increment operations (atomic counter).
 * Uses RBucket for string storage (put/get).
 *
 * Note: Due to Redis type system, a key can only be one type.
 * Keys used with increment() should only be accessed via increment/get operations.
 * Keys used with put() should only be accessed via put/get operations.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisKvStore implements IKvStore, IKvSortedSet {

    private static final String SORTED_SET_ADD_SCRIPT = """
            redis.call('zadd', KEYS[1], ARGV[1], ARGV[2])
            redis.call('expire', KEYS[1], ARGV[3])
            return 1
            """;
    private static final String SORTED_SET_RANGE_SCRIPT = """
            if ARGV[3] == '0' then
              return redis.call('zrangebyscore', KEYS[1], ARGV[1], ARGV[2])
            end
            return redis.call('zrangebyscore', KEYS[1], ARGV[1], ARGV[2], 'LIMIT', 0, ARGV[3])
            """;

    private final RedissonClient redissonClient;

    @Override
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            return putNonPositiveTtl(key);
        }
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.setIfAbsent(value, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public void set(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            putNonPositiveTtl(key);
            return;
        }
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(value, Duration.ofSeconds(expireSeconds));
    }

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        return setIfAbsent(key, value, expireSeconds);
    }

    @Override
    public String get(String key) {
        validateKey(key);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        return bucket.get();
    }

    @Override
    public long incrementBy(String key, long delta, long windowSeconds) {
        validateKey(key);
        Require.positive(windowSeconds, "windowSeconds must be positive, was: " + windowSeconds);
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        long count = atomic.addAndGet(delta);
        // Set TTL on first increment of a new key.
        // Use expireAsync to avoid blocking
        if (count == delta) {
            atomic.expire(Duration.ofSeconds(windowSeconds));
        }
        return count;
    }

    @Override
    public long increment(String key, long windowSeconds) {
        return incrementBy(key, 1, windowSeconds);
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        redissonClient.getKeys().delete(key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        return redissonClient.getKeys().countExists(key) > 0;
    }

    @Override
    public void add(String key, String member, double score, long ttlSeconds) {
        validateKey(key);
        validateKey(member);
        if (ttlSeconds <= 0) {
            remove(key, member);
            return;
        }
        script().eval(
                RScript.Mode.READ_WRITE,
                SORTED_SET_ADD_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(key),
                scoreValue(score),
                member,
                String.valueOf(ttlSeconds));
    }

    @Override
    public void remove(String key, String member) {
        validateKey(key);
        validateKey(member);
        script().eval(
                RScript.Mode.READ_WRITE,
                "return redis.call('zrem', KEYS[1], ARGV[1])",
                RScript.ReturnType.INTEGER,
                List.of(key),
                member);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> rangeByScore(String key, double minScore, double maxScore, int limit) {
        validateKey(key);
        return (Collection<String>) script().eval(
                RScript.Mode.READ_ONLY,
                SORTED_SET_RANGE_SCRIPT,
                RScript.ReturnType.MULTI,
                List.of(key),
                scoreValue(minScore),
                scoreValue(maxScore),
                String.valueOf(Math.max(limit, 0)));
    }

    @Override
    public long removeByScore(String key, double minScore, double maxScore) {
        validateKey(key);
        Number result = script().eval(
                RScript.Mode.READ_WRITE,
                "return redis.call('zremrangebyscore', KEYS[1], ARGV[1], ARGV[2])",
                RScript.ReturnType.INTEGER,
                List.of(key),
                scoreValue(minScore),
                scoreValue(maxScore));
        return result.longValue();
    }

    @Override
    public long size(String key) {
        validateKey(key);
        Number result = script().eval(
                RScript.Mode.READ_ONLY,
                "return redis.call('zcard', KEYS[1])",
                RScript.ReturnType.INTEGER,
                List.of(key));
        return result.longValue();
    }

    private RScript script() {
        return redissonClient.getScript(StringCodec.INSTANCE);
    }

    private String scoreValue(double score) {
        if (score == Double.NEGATIVE_INFINITY) {
            return "-inf";
        }
        if (score == Double.POSITIVE_INFINITY) {
            return "+inf";
        }
        return Double.toString(score);
    }

    private void validateKey(String key) {
        Require.notBlank(key, "key cannot be null or blank");
    }
}
