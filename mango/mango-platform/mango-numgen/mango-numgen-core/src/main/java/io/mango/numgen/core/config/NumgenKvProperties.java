package io.mango.numgen.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mango.numgen.kv")
public class NumgenKvProperties {

    /**
     * Active rule expression cache TTL.
     */
    private long ruleCacheTtlSeconds = 300L;

    public long getRuleCacheTtlSeconds() {
        return ruleCacheTtlSeconds;
    }

    public void setRuleCacheTtlSeconds(long ruleCacheTtlSeconds) {
        this.ruleCacheTtlSeconds = ruleCacheTtlSeconds;
    }
}
