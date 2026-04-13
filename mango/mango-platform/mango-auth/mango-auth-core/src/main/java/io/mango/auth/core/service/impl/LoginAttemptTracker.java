package io.mango.auth.core.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks failed login attempts to prevent brute force attacks.
 * Uses in-memory storage with automatic expiration.
 *
 * @author Mango
 */
@Slf4j
public class LoginAttemptTracker {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    private static final long WINDOW_MINUTES = 10;

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;

    public LoginAttemptTracker(ScheduledExecutorService cleanupScheduler) {
        this.cleanupScheduler = cleanupScheduler;
        // Cleanup expired entries every minute
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredAttempts, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Record a failed login attempt
     *
     * @param key IP address or username
     */
    public void recordFailedAttempt(String key) {
        LoginAttempt attempt = attempts.computeIfAbsent(key, k -> new LoginAttempt());
        attempt.recordFailure();
        log.debug("Failed login attempt recorded for {}: {} attempts", key, attempt.count);
    }

    /**
     * Check if the key is locked out
     *
     * @param key IP address or username
     * @return true if locked out
     */
    public boolean isLockedOut(String key) {
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
     * Clear attempts on successful login
     *
     * @param key IP address or username
     */
    public void clearAttempts(String key) {
        attempts.remove(key);
        log.debug("Login attempts cleared for {}", key);
    }

    /**
     * Get remaining lockout time in minutes
     *
     * @param key IP address or username
     * @return remaining minutes, or 0 if not locked out
     */
    public long getRemainingLockoutMinutes(String key) {
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

    /**
     * Internal class to track login attempts
     */
    private static class LoginAttempt {
        private int count = 0;
        private Instant windowStart = Instant.now();

        synchronized void recordFailure() {
            Instant now = Instant.now();
            // Reset window if expired
            if (now.isAfter(windowStart.plusSeconds(WINDOW_MINUTES * 60L))) {
                windowStart = now;
                count = 1;
            } else {
                count++;
            }
        }

        synchronized boolean isLockedOut(long lockoutMinutes, long windowMinutes) {
            Instant now = Instant.now();
            // Reset if window expired
            if (now.isAfter(windowStart.plusSeconds(windowMinutes * 60L))) {
                return false;
            }
            return count >= MAX_ATTEMPTS;
        }

        synchronized long getRemainingLockoutMinutes(long lockoutMinutes, long windowMinutes) {
            if (!isLockedOut(lockoutMinutes, windowMinutes)) {
                return 0;
            }
            // Calculate when the window will expire
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
