package io.mango.auth.core.anti;

import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Idempotency guard using IKvStore.
 * Ensures duplicate POST/PUT/DELETE requests return cached results.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private static final long IDEM_TTL_SECONDS = 86400; // 24 hours
    private static final String KEY_PREFIX = "idem:";
    private static final String PROCESSING = "PROCESSING";

    private final IKvStore kvStore;


    /**
     * Try to acquire idempotency key for a request.
     * @param key the idempotency key (e.g., X-Idempotency-Key header value)
     * @return true=first request (proceed), false=duplicate (return cached)
     */
    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("idempotency key cannot be null or blank");
        }
        String fullKey = KEY_PREFIX + key;
        boolean acquired = kvStore.setIfAbsent(fullKey, PROCESSING, IDEM_TTL_SECONDS);
        if (!acquired) {
            log.info("Idempotent request detected: key={}", key);
        }
        return acquired;
    }

    /**
     * Save response for an idempotency key.
     * @param key the idempotency key
     * @param response the response body to cache
     */
    public void saveResponse(String key, String response) {
        if (key == null || key.isBlank()) {
            return;
        }
        kvStore.set(KEY_PREFIX + key, response, IDEM_TTL_SECONDS);
    }

    /**
     * Get cached response for an idempotency key.
     * @param key the idempotency key
     * @return cached response or null if not found
     */
    public String getResponse(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return kvStore.get(KEY_PREFIX + key);
    }
}
