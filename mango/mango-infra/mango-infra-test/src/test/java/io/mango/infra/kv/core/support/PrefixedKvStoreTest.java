package io.mango.infra.kv.core.support;

import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrefixedKvStoreTest {

    @Test
    void put_get_delete_exists_useNormalizedKey() {
        RecordingKvStore delegate = new RecordingKvStore();
        KvKeyNormalizer normalizer = new KvKeyNormalizer(true, "mango:kv", "test", false, null);
        PrefixedKvStore store = new PrefixedKvStore(delegate, normalizer, KvKeyNormalizer.CACHE);

        store.put("user:1", "value", 60);
        store.get("user:1");
        store.exists("user:1");
        store.delete("user:1");

        assertThat(delegate.lastKey).isEqualTo("mango:kv:test:cache:user:1");
    }

    @Test
    void increment_usesNormalizedKey() {
        RecordingKvStore delegate = new RecordingKvStore();
        KvKeyNormalizer normalizer = new KvKeyNormalizer(true, "mango:kv", "prod", false, null);
        PrefixedKvStore store = new PrefixedKvStore(delegate, normalizer, KvKeyNormalizer.RATE_LIMIT);

        store.increment("login:ip:127.0.0.1", 60);

        assertThat(delegate.lastKey).isEqualTo("mango:kv:prod:rate-limit:login:ip:127.0.0.1");
    }

    private static class RecordingKvStore implements IKvStore {
        private String lastKey;

        @Override
        public boolean put(String key, String value, long expireSeconds) {
            lastKey = key;
            return true;
        }

        @Override
        public String get(String key) {
            lastKey = key;
            return null;
        }

        @Override
        public long increment(String key, long windowSeconds) {
            lastKey = key;
            return 1;
        }

        @Override
        public void delete(String key) {
            lastKey = key;
        }

        @Override
        public boolean exists(String key) {
            lastKey = key;
            return false;
        }
    }
}
