package cn.keking.service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** 进程内租约，适用于单体部署或未启用共享缓存的独立引擎。 */
@Component
@ConditionalOnExpression("!'${cache.type:default}'.equals('redis')")
public final class LocalConversionTaskLease implements ConversionTaskLease {
    private final ConcurrentMap<String, Long> leases = new ConcurrentHashMap<>();
    @Override
    public boolean tryAcquire(String key, Duration leaseDuration) {
        long expiresAt = System.nanoTime() + leaseDuration.toNanos();
        return leases.compute(key, (ignored, currentExpiry) ->
                currentExpiry == null || currentExpiry <= System.nanoTime() ? expiresAt : currentExpiry) == expiresAt;
    }
    @Override
    public void release(String key) { leases.remove(key); }
}
