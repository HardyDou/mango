package io.mango.infra.kv.core.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KvKeyNormalizerTest {

    @Test
    void normalize_defaultNamespace_addsEnvAndCapability() {
        KvKeyNormalizer normalizer = new KvKeyNormalizer(true, "mango:kv", "prod", false, null);

        assertThat(normalizer.normalize(KvKeyNormalizer.CACHE, "user:1"))
                .isEqualTo("mango:kv:prod:cache:user:1");
    }

    @Test
    void normalize_withAppEnabled_addsAppSegment() {
        KvKeyNormalizer normalizer = new KvKeyNormalizer(true, "mango:kv", "prod", true, "admin");

        assertThat(normalizer.normalize(KvKeyNormalizer.LOCK, "order:1"))
                .isEqualTo("mango:kv:prod:admin:lock:order:1");
    }

    @Test
    void normalize_alreadyPrefixedForSameCapability_keepsOriginal() {
        KvKeyNormalizer normalizer = new KvKeyNormalizer(true, "mango:kv", "prod", false, null);

        assertThat(normalizer.normalize(KvKeyNormalizer.CACHE, "mango:kv:prod:cache:user:1"))
                .isEqualTo("mango:kv:prod:cache:user:1");
    }

    @Test
    void normalize_disabled_returnsRawKey() {
        KvKeyNormalizer normalizer = new KvKeyNormalizer(false, "mango:kv", "prod", false, null);

        assertThat(normalizer.normalize(KvKeyNormalizer.CACHE, "user:1")).isEqualTo("user:1");
    }

    @Test
    void normalize_invalidEnv_throws() {
        assertThatThrownBy(() -> new KvKeyNormalizer(true, "mango:kv", "prod:blue", false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("env");
    }
}
