package io.mango.infra.iplocation.core.cache;

import io.mango.infra.iplocation.api.IpLocation;
import io.mango.infra.iplocation.api.IpLocationResolver;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量本地缓存解析器。
 */
public class CachingIpLocationResolver implements IpLocationResolver {

    private final IpLocationResolver delegate;
    private final int maximumSize;
    private final long ttlMillis;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public CachingIpLocationResolver(IpLocationResolver delegate, int maximumSize, Duration ttl) {
        this.delegate = delegate;
        this.maximumSize = Math.max(1, maximumSize);
        this.ttlMillis = ttl == null ? Duration.ofHours(24).toMillis() : ttl.toMillis();
    }

    @Override
    public IpLocation resolve(String ip) {
        String key = ip == null ? "" : ip.trim();
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(key);
        if (entry != null && now - entry.createdAt <= ttlMillis) {
            return entry.location;
        }
        IpLocation location = delegate.resolve(ip);
        if (cache.size() >= maximumSize) {
            cache.clear();
        }
        cache.put(key, new CacheEntry(location, now));
        return location;
    }

    private record CacheEntry(IpLocation location, long createdAt) {
    }
}
