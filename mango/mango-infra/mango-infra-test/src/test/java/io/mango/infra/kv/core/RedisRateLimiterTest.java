package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis IRateLimiter tests using in-memory IKvStore mock.
 */
public class RedisRateLimiterTest {

    private IRateLimiter rateLimiter;
    private MemoryKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        rateLimiter = new RedisRateLimiter(kvStore);
    }

    @Test
    void tryAcquire_shouldAcquirePermit() {
        assertThat(rateLimiter.tryAcquire("limiter1", 1)).isTrue();
    }

    @Test
    void tryAcquire_withNullKey_shouldThrow() {
        assertThatThrownBy(() -> rateLimiter.tryAcquire(null, 1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryAcquire_withZeroPermits_shouldThrow() {
        assertThatThrownBy(() -> rateLimiter.tryAcquire("limiter1", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
