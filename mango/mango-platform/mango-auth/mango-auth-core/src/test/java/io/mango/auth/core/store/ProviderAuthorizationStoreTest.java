package io.mango.auth.core.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.enums.ExternalAuthProvider;
import io.mango.auth.api.enums.ProviderAuthorizationIntent;
import io.mango.common.exception.BizException;
import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderAuthorizationStoreTest {

    @Test
    void stateAndBindingTicketsAreOneTimeAndKeepTheirScopedPayload() {
        ProviderAuthorizationStore store = store(new AtomicKvStore());
        var statePayload = new ProviderAuthorizationStore.StatePayload(
                "1", "internal-admin", ExternalAuthProvider.WECOM,
                ProviderAuthorizationIntent.BIND_CURRENT, "https://admin.example.com/callback", 7L);
        var bindingPayload = new ProviderAuthorizationStore.BindingPayload(
                "1", "internal-admin", ExternalAuthProvider.DINGTALK, "corp", "external", "用户");

        String state = store.issueState(statePayload);
        String ticket = store.issueBinding(bindingPayload);

        assertThat(store.consumeState(state)).isEqualTo(statePayload);
        assertThat(store.consumeBinding(ticket)).isEqualTo(bindingPayload);
        assertThatThrownBy(() -> store.consumeState(state)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> store.consumeBinding(ticket)).isInstanceOf(BizException.class);
    }

    @Test
    void concurrentConsumersCannotBothUseTheSameState() throws Exception {
        ProviderAuthorizationStore store = store(new AtomicKvStore());
        String state = store.issueState(new ProviderAuthorizationStore.StatePayload(
                "1", "internal-admin", ExternalAuthProvider.WECOM,
                ProviderAuthorizationIntent.LOGIN, "https://admin.example.com/callback", null));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(2)) {
            for (int index = 0; index < 2; index++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        store.consumeState(state);
                        successes.incrementAndGet();
                    } catch (BizException ignored) {
                        // Expected for the losing consumer.
                    }
                    return null;
                });
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(successes).hasValue(1);
    }

    private ProviderAuthorizationStore store(IKvStore kvStore) {
        ProviderAuthorizationStore store = new ProviderAuthorizationStore(kvStore, new ObjectMapper());
        ReflectionTestUtils.setField(store, "stateTtlSeconds", 600L);
        ReflectionTestUtils.setField(store, "bindTicketTtlSeconds", 600L);
        return store;
    }

    private static final class AtomicKvStore implements IKvStore {
        private final Map<String, String> values = new ConcurrentHashMap<>();

        @Override
        public boolean setIfAbsent(String key, String value, long expireSeconds) {
            return values.putIfAbsent(key, value) == null;
        }

        @Override
        public String get(String key) {
            return values.get(key);
        }

        @Override
        public void delete(String key) {
            values.remove(key);
        }

        @Override
        public boolean deleteIfValue(String key, String expectedValue) {
            return values.remove(key, expectedValue);
        }

        @Override
        public boolean exists(String key) {
            return values.containsKey(key);
        }
    }
}
