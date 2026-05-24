package io.mango.payment.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mango.payment.kv")
public class PaymentKvProperties {

    /**
     * Payment/refund request idempotency window.
     */
    private long idempotentWindowSeconds = 86400L;

    /**
     * Channel notify duplicate-detection window.
     */
    private long notifyIdempotentWindowSeconds = 604800L;

    /**
     * Business order state transition lock TTL.
     */
    private long bizOrderLockTtlSeconds = 30L;

    public long getIdempotentWindowSeconds() {
        return idempotentWindowSeconds;
    }

    public void setIdempotentWindowSeconds(long idempotentWindowSeconds) {
        this.idempotentWindowSeconds = idempotentWindowSeconds;
    }

    public long getNotifyIdempotentWindowSeconds() {
        return notifyIdempotentWindowSeconds;
    }

    public void setNotifyIdempotentWindowSeconds(long notifyIdempotentWindowSeconds) {
        this.notifyIdempotentWindowSeconds = notifyIdempotentWindowSeconds;
    }

    public long getBizOrderLockTtlSeconds() {
        return bizOrderLockTtlSeconds;
    }

    public void setBizOrderLockTtlSeconds(long bizOrderLockTtlSeconds) {
        this.bizOrderLockTtlSeconds = bizOrderLockTtlSeconds;
    }
}
