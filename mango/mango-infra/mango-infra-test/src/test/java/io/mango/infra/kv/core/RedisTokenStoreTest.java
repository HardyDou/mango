package io.mango.infra.kv.core;

import io.mango.infra.kv.api.ITokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis ITokenStore tests using in-memory IKvStore mock.
 */
public class RedisTokenStoreTest {

    private ITokenStore tokenStore;
    private MemoryKvStore kvStore;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        tokenStore = new RedisTokenStore(kvStore);
    }

    @Test
    void store_shouldStoreToken() {
        tokenStore.store("token1", "user123", 3600);
        assertThat(tokenStore.get("token1")).isEqualTo("user123");
    }

    @Test
    void get_shouldReturnNullWhenNotFound() {
        assertThat(tokenStore.get("nonexistent")).isNull();
    }

    @Test
    void remove_shouldDeleteToken() {
        tokenStore.store("token1", "user123", 3600);
        tokenStore.remove("token1");
        assertThat(tokenStore.get("token1")).isNull();
    }

    @Test
    void store_withNullToken_shouldThrow() {
        assertThatThrownBy(() -> tokenStore.store(null, "value", 60))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_withNullValue_shouldThrow() {
        assertThatThrownBy(() -> tokenStore.store("token", null, 60))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_withZeroTtl_shouldThrow() {
        assertThatThrownBy(() -> tokenStore.store("token", "value", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
