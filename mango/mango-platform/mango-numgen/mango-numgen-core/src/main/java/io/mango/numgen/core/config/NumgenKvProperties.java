package io.mango.numgen.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mango.numgen.kv")
public class NumgenKvProperties {

    /**
     * Active rule expression cache TTL.
     */
    private long ruleCacheTtlSeconds = 300L;

    /**
     * Retained for configuration compatibility. Sequence allocation now uses a database atomic upsert.
     */
    @Deprecated(forRemoval = true)
    private long allocationLockTtlSeconds = 10L;

    public long getRuleCacheTtlSeconds() {
        return ruleCacheTtlSeconds;
    }

    public void setRuleCacheTtlSeconds(long ruleCacheTtlSeconds) {
        this.ruleCacheTtlSeconds = ruleCacheTtlSeconds;
    }

    @Deprecated(forRemoval = true)
    public long getAllocationLockTtlSeconds() {
        return allocationLockTtlSeconds;
    }

    @Deprecated(forRemoval = true)
    public void setAllocationLockTtlSeconds(long allocationLockTtlSeconds) {
        this.allocationLockTtlSeconds = allocationLockTtlSeconds;
    }
}
