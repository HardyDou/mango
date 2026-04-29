package io.mango.auth.core.anti;

import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 IKvStore 的重放攻击保护器。
 * 每个请求 nonce 保存 10 分钟。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplayGuard {

    private static final long NONCE_TTL_SECONDS = 600; // 10 分钟
    private static final String KEY_PREFIX = "replay:";

    private final IKvStore kvStore;

    /**
     * 原子占用 nonce。
     * @param nonce 请求唯一 nonce
     * @return true 表示新请求，false 表示重复请求
     */
    public boolean tryAcquire(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("nonce cannot be null or blank");
        }
        String key = KEY_PREFIX + nonce;
        boolean acquired = kvStore.setIfAbsent(key, "1", NONCE_TTL_SECONDS);
        if (!acquired) {
            log.warn("Replay attack detected: nonce={}", nonce);
        }
        return acquired;
    }

    /**
     * 检查 nonce 是否已使用。
     * @param nonce 请求唯一 nonce
     * @return true 表示已使用，false 表示未使用
     */
    public boolean isUsed(String nonce) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }
        return kvStore.get(KEY_PREFIX + nonce) != null;
    }
}
