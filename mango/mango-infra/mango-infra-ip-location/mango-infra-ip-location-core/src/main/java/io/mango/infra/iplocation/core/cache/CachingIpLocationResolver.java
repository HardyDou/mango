package io.mango.infra.iplocation.core.cache;

import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 轻量本地缓存解析器。
 */
public class CachingIpLocationResolver implements IpLocationResolver {

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final IpLocationResolver delegate;
    private final int maximumSize;
    private final long ttlNanos;
    private final LongSupplier ticker;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public CachingIpLocationResolver(IpLocationResolver delegate, int maximumSize, Duration ttl) {
        this(delegate, maximumSize, ttl, System::nanoTime);
    }

    CachingIpLocationResolver(IpLocationResolver delegate, int maximumSize, Duration ttl, LongSupplier ticker) {
        this.delegate = delegate;
        this.maximumSize = Math.max(1, maximumSize);
        Duration effectiveTtl = ttl;
        if (effectiveTtl == null) {
            effectiveTtl = DEFAULT_TTL;
        }
        this.ttlNanos = Math.max(0, effectiveTtl.toNanos());
        this.ticker = ticker;
    }

    @Override
    public IpLocation resolve(String ip) {
        String key = "";
        if (ip != null) {
            key = ip.trim();
        }
        long now = ticker.getAsLong();
        CacheEntry entry = cache.get(key);
        if (entry != null && now - entry.createdAt <= ttlNanos) {
            return IpLocation.copyOf(entry.location);
        }
        IpLocation resolved;
        try {
            resolved = delegate.resolve(key);
        } catch (RuntimeException e) {
            resolved = null;
        }
        IpLocation location = IpLocation.copyOf(resolved);
        if (location == null) {
            location = IpLocation.empty(key);
        }
        if (cache.size() >= maximumSize) {
            cache.clear();
        }
        cache.put(key, new CacheEntry(IpLocation.copyOf(location), now));
        return IpLocation.copyOf(location);
    }

    private record CacheEntry(IpLocation location, long createdAt) {
    }
}
