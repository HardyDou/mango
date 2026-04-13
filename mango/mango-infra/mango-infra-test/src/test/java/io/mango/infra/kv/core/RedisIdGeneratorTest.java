package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis IIdGenerator tests using in-memory IKvStore mock.
 */
public class RedisIdGeneratorTest {

    private IIdGenerator idGenerator;
    private MemoryKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        idGenerator = new RedisIdGenerator(kvStore);
    }

    @Test
    void nextId_shouldReturnIncrementingIds() {
        assertThat(idGenerator.nextId()).isEqualTo(1);
        assertThat(idGenerator.nextId()).isEqualTo(2);
        assertThat(idGenerator.nextId()).isEqualTo(3);
    }

    @Test
    void nextId_withSpecificKey_shouldReturnIncrementingIds() {
        assertThat(((RedisIdGenerator) idGenerator).nextId("order")).isEqualTo(1);
        assertThat(((RedisIdGenerator) idGenerator).nextId("order")).isEqualTo(2);
        assertThat(((RedisIdGenerator) idGenerator).nextId("user")).isEqualTo(1);
    }
}
