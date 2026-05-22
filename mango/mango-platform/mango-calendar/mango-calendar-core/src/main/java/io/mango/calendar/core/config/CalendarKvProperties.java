package io.mango.calendar.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mango.calendar.kv")
public class CalendarKvProperties {

    /**
     * Annual day-result cache TTL.
     */
    private long dayCacheTtlSeconds = 86400L;

    public long getDayCacheTtlSeconds() {
        return dayCacheTtlSeconds;
    }

    public void setDayCacheTtlSeconds(long dayCacheTtlSeconds) {
        this.dayCacheTtlSeconds = dayCacheTtlSeconds;
    }
}
