package io.mango.auth.core.store;

import io.mango.common.exception.BizException;
import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTicketStoreTest {

    @Test
    void consumeReturnsPayloadAndDeletesTicketFromKv() {
        InMemoryKvStore kvStore = new InMemoryKvStore();
        PasswordResetTicketStore store = new PasswordResetTicketStore(kvStore);
        PasswordResetTicketStore.TicketPayload payload = new PasswordResetTicketStore.TicketPayload(
                1001L, "1", "default", "internal-admin",
                "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 2001L);

        String ticket = store.issue(payload);
        PasswordResetTicketStore.TicketPayload consumed = store.consume(ticket);

        assertThat(consumed).isEqualTo(payload);
        assertThat(kvStore.exists("auth:password-reset-ticket:" + ticket)).isFalse();
        assertThatThrownBy(() -> store.consume(ticket))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("强制改密凭据无效或已过期");
    }

    @Test
    void peekReturnsPayloadWithoutDeletingTicketFromKv() {
        InMemoryKvStore kvStore = new InMemoryKvStore();
        PasswordResetTicketStore store = new PasswordResetTicketStore(kvStore);
        PasswordResetTicketStore.TicketPayload payload = new PasswordResetTicketStore.TicketPayload(
                1001L, "1", "default", "internal-admin",
                "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 2001L);

        String ticket = store.issue(payload);
        PasswordResetTicketStore.TicketPayload peeked = store.peek(ticket);

        assertThat(peeked).isEqualTo(payload);
        assertThat(kvStore.exists("auth:password-reset-ticket:" + ticket)).isTrue();

        store.revoke(ticket);
        assertThat(kvStore.exists("auth:password-reset-ticket:" + ticket)).isFalse();
    }

    private static final class InMemoryKvStore implements IKvStore {
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
        public boolean exists(String key) {
            return values.containsKey(key);
        }
    }
}
