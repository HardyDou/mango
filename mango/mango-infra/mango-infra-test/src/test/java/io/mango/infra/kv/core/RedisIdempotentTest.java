package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IIdempotent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis IIdempotent tests using in-memory IKvStore mock.
 */
public class RedisIdempotentTest {

    private IIdempotent idempotent;
    private MemoryKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        idempotent = new RedisIdempotent(kvStore);
    }

    @Test
    void isDuplicate_shouldReturnFalseWhenNotMarked() {
        assertThat(idempotent.isDuplicate("key1", 60)).isFalse();
    }

    @Test
    void isDuplicate_shouldReturnTrueWhenMarked() {
        idempotent.mark("key1", 60);
        assertThat(idempotent.isDuplicate("key1", 60)).isTrue();
    }

    @Test
    void checkAndMark_shouldReturnFalseWhenNew() {
        assertThat(idempotent.checkAndMark("key1", 60)).isFalse();
    }

    @Test
    void checkAndMark_shouldReturnTrueWhenDuplicate() {
        idempotent.checkAndMark("key1", 60);
        assertThat(idempotent.checkAndMark("key1", 60)).isTrue();
    }

    @Test
    void mark_withNullKey_shouldThrow() {
        assertThatThrownBy(() -> idempotent.mark(null, 60))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isDuplicate_withZeroWindow_shouldThrow() {
        assertThatThrownBy(() -> idempotent.isDuplicate("key1", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
