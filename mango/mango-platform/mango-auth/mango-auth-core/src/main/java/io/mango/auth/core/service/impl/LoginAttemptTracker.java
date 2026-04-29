package io.mango.auth.core.service.impl;

import io.mango.infra.kv.api.IKvStore;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败次数追踪器，用于防止暴力破解。
 * 默认使用内存存储并自动清理过期数据。
 *
 * @author Mango
 */
@Slf4j
public class LoginAttemptTracker {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    private static final long WINDOW_MINUTES = 10;
    private static final String KEY_PREFIX = "auth:login:attempt:";

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;
    private final IKvStore kvStore;

    public LoginAttemptTracker(ScheduledExecutorService cleanupScheduler) {
        this(null, cleanupScheduler);
    }

    public LoginAttemptTracker(IKvStore kvStore, ScheduledExecutorService cleanupScheduler) {
        this.kvStore = kvStore;
        this.cleanupScheduler = cleanupScheduler;
        // 每分钟清理一次过期记录。
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredAttempts, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 记录一次登录失败。
     *
     * @param key IP 地址或用户名
     */
    public void recordFailedAttempt(String key) {
        if (kvStore != null) {
            String storeKey = kvKey(key);
            long count = parseCount(kvStore.get(storeKey)) + 1;
            kvStore.set(storeKey, String.valueOf(count), WINDOW_MINUTES * 60L);
            log.debug("Failed login attempt recorded for {}: {} attempts", key, count);
            return;
        }
        LoginAttempt attempt = attempts.computeIfAbsent(key, k -> new LoginAttempt());
        attempt.recordFailure();
        log.debug("Failed login attempt recorded for {}: {} attempts", key, attempt.count);
    }

    /**
     * 判断指定 key 是否已被锁定。
     *
     * @param key IP 地址或用户名
     * @return 是否已锁定
     */
    public boolean isLockedOut(String key) {
        if (kvStore != null) {
            String value = kvStore.get(kvKey(key));
            return value != null && Long.parseLong(value) >= MAX_ATTEMPTS;
        }
        LoginAttempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.isLockedOut(LOCKOUT_DURATION_MINUTES, WINDOW_MINUTES)) {
            log.debug("Login is locked out for {} due to too many failed attempts", key);
            return true;
        }
        return false;
    }

    /**
     * 登录成功后清理失败次数。
     *
     * @param key IP 地址或用户名
     */
    public void clearAttempts(String key) {
        if (kvStore != null) {
            kvStore.delete(kvKey(key));
            return;
        }
        attempts.remove(key);
        log.debug("Login attempts cleared for {}", key);
    }

    /**
     * 获取剩余锁定时间。
     *
     * @param key IP 地址或用户名
     * @return 剩余分钟数，未锁定时返回 0
     */
    public long getRemainingLockoutMinutes(String key) {
        if (kvStore != null) {
            return isLockedOut(key) ? LOCKOUT_DURATION_MINUTES : 0;
        }
        LoginAttempt attempt = attempts.get(key);
        if (attempt == null) {
            return 0;
        }
        return attempt.getRemainingLockoutMinutes(LOCKOUT_DURATION_MINUTES, WINDOW_MINUTES);
    }

    private void cleanupExpiredAttempts() {
        int before = attempts.size();
        Instant cutoff = Instant.now().minusSeconds(WINDOW_MINUTES * 60L);
        attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(cutoff));
        int removed = before - attempts.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired login attempts", removed);
        }
    }

    public void shutdown() {
        cleanupScheduler.shutdown();
    }

    private String kvKey(String key) {
        return KEY_PREFIX + key;
    }

    private long parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 登录失败记录。
     */
    private static class LoginAttempt {
        private int count = 0;
        private Instant windowStart = Instant.now();

        synchronized void recordFailure() {
            Instant now = Instant.now();
            // 窗口过期后重新计数。
            if (now.isAfter(windowStart.plusSeconds(WINDOW_MINUTES * 60L))) {
                windowStart = now;
                count = 1;
            } else {
                count++;
            }
        }

        synchronized boolean isLockedOut(long lockoutMinutes, long windowMinutes) {
            Instant now = Instant.now();
            // 窗口过期时视为未锁定。
            if (now.isAfter(windowStart.plusSeconds(windowMinutes * 60L))) {
                return false;
            }
            return count >= MAX_ATTEMPTS;
        }

        synchronized long getRemainingLockoutMinutes(long lockoutMinutes, long windowMinutes) {
            if (!isLockedOut(lockoutMinutes, windowMinutes)) {
                return 0;
            }
            // 计算当前窗口剩余时间。
            Instant windowEnd = windowStart.plusSeconds(windowMinutes * 60L);
            Instant now = Instant.now();
            long remainingSeconds = java.time.Duration.between(now, windowEnd).getSeconds();
            return Math.max(0, remainingSeconds / 60);
        }

        synchronized boolean isExpired(Instant cutoff) {
            return windowStart.isBefore(cutoff);
        }
    }
}
