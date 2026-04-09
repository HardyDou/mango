package io.mango.auth.core.anti;

import io.mango.dal.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Replay attack guard using IKvStore.
 * Each request nonce is stored with 10-minute TTL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplayGuard {

    private static final long NONCE_TTL_SECONDS = 600; // 10 minutes
    private static final String KEY_PREFIX = "replay:";

    private final IKvStore kvStore;

    /**
     * Atomically try to acquire a nonce.
     * @param nonce the unique request nonce
     * @return true=new request (allow), false=duplicate request (reject)
     */
    public boolean tryAcquire(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("nonce cannot be null or blank");
        }
        String key = KEY_PREFIX + nonce;
        boolean acquired = kvStore.put(key, "1", NONCE_TTL_SECONDS);
        if (!acquired) {
            log.warn("Replay attack detected: nonce={}", nonce);
        }
        return acquired;
    }

    /**
     * Check if a nonce has been used.
     * @param nonce the unique request nonce
     * @return true=already used, false=fresh
     */
    public boolean isUsed(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }
        return kvStore.get(KEY_PREFIX + nonce) != null;
    }
}
