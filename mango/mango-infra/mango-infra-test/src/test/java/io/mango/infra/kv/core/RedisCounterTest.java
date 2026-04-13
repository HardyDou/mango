package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ICounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis ICounter tests using in-memory IKvStore mock.
 */
public class RedisCounterTest {

    private ICounter counter;
    private MemoryKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        counter = new RedisCounter(kvStore);
    }

    @Test
    void increment_shouldIncrementByOne() {
        assertThat(counter.increment("counter1", 1, 60)).isEqualTo(1);
        assertThat(counter.increment("counter1", 1, 60)).isEqualTo(2);
    }

    @Test
    void get_shouldReturnZeroWhenNotFound() {
        assertThat(counter.get("nonexistent")).isEqualTo(0);
    }

    @Test
    void get_shouldReturnCurrentValue() {
        counter.increment("counter1", 1, 60);
        counter.increment("counter1", 1, 60);
        assertThat(counter.get("counter1")).isEqualTo(2);
    }

    @Test
    void increment_withNullKey_shouldThrow() {
        assertThatThrownBy(() -> counter.increment(null, 1, 60))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void increment_withZeroWindow_shouldThrow() {
        assertThatThrownBy(() -> counter.increment("counter1", 1, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
