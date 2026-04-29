package io.mango.auth.core.anti;

import io.mango.infra.kv.api.IKvStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 IKvStore 的幂等保护器。
 * 确保重复的 POST/PUT/DELETE 请求返回缓存结果。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private static final long IDEM_TTL_SECONDS = 86400; // 24 小时
    private static final String KEY_PREFIX = "idem:";
    private static final String PROCESSING = "PROCESSING";

    private final IKvStore kvStore;


    /**
     * 尝试占用请求幂等键。
     * @param key 幂等键，例如 X-Idempotency-Key 请求头
     * @return true 表示首次请求，false 表示重复请求
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
     * 保存幂等键对应的响应。
     * @param key 幂等键
     * @param response 待缓存响应体
     */
    public void saveResponse(String key, String response) {
        if (key == null || key.isBlank()) {
            return;
        }
        kvStore.set(KEY_PREFIX + key, response, IDEM_TTL_SECONDS);
    }

    /**
     * 获取幂等键对应的缓存响应。
     * @param key 幂等键
     * @return 缓存响应，未命中时返回 null
     */
    public String getResponse(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return kvStore.get(KEY_PREFIX + key);
    }
}
