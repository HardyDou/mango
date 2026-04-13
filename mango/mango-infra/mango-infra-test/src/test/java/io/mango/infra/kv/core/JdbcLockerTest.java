package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ILocker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JDBC ILocker tests using in-memory IKvStore mock.
 */
public class JdbcLockerTest {

    private ILocker locker;
    private MemoryKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        locker = new JdbcLocker(kvStore);
    }

    @Test
    void tryLock_shouldAcquireLock() {
        assertThat(locker.tryLock("lock1", 60)).isTrue();
    }

    @Test
    void tryLock_shouldFailWhenAlreadyLocked() {
        locker.tryLock("lock1", 60);
        assertThat(locker.tryLock("lock1", 60)).isFalse();
    }

    @Test
    void unlock_shouldReleaseLock() {
        locker.tryLock("lock1", 60);
        locker.unlock("lock1");
        assertThat(locker.tryLock("lock1", 60)).isTrue();
    }

    @Test
    void tryLock_withNullKey_shouldThrow() {
        assertThatThrownBy(() -> locker.tryLock(null, 60))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryLock_withZeroTtl_shouldThrow() {
        assertThatThrownBy(() -> locker.tryLock("lock1", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
