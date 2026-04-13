package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ICache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis ICache tests using in-memory IKvStore mock.
 */
public class RedisCacheTest {

    private ICache cache;
    private MemoryKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        cache = new RedisCache(kvStore);
    }

    @Test
    void set_shouldStoreValue() {
        cache.set("key1", "value1", 60);
        assertThat(cache.get("key1")).isEqualTo("value1");
    }

    @Test
    void get_shouldReturnNullWhenNotFound() {
        assertThat(cache.get("nonexistent")).isNull();
    }

    @Test
    void exists_shouldReturnTrueWhenExists() {
        cache.set("key1", "value1", 60);
        assertThat(cache.exists("key1")).isTrue();
    }

    @Test
    void exists_shouldReturnFalseWhenNotExists() {
        assertThat(cache.exists("nonexistent")).isFalse();
    }

    @Test
    void delete_shouldRemoveValue() {
        cache.set("key1", "value1", 60);
        cache.delete("key1");
        assertThat(cache.get("key1")).isNull();
    }

    @Test
    void set_withNullKey_shouldThrow() {
        assertThatThrownBy(() -> cache.set(null, "value", 60))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void set_withNullValue_shouldThrow() {
        assertThatThrownBy(() -> cache.set("key", null, 60))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void set_withZeroTtl_shouldThrow() {
        assertThatThrownBy(() -> cache.set("key", "value", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
