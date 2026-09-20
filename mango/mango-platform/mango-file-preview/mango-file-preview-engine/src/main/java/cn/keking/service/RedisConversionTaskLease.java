package cn.keking.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Redis 租约，保证独立预览引擎多实例不会并发转换同一版本文件。 */
@Component
@ConditionalOnExpression("'${cache.type:default}'.equals('redis')")
public class RedisConversionTaskLease implements ConversionTaskLease {
    private static final String PREFIX = "mango:file-preview:conversion:";
    private final RedissonClient redissonClient;

    public RedisConversionTaskLease(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryAcquire(String key, Duration leaseDuration) throws InterruptedException {
        RLock lock = redissonClient.getLock(PREFIX + key);
        return lock.tryLock(0, leaseDuration.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void release(String key) {
        RLock lock = redissonClient.getLock(PREFIX + key);
        if (lock.isHeldByCurrentThread()) lock.unlock();
    }
}
