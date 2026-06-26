package io.mango.auth.core.service.impl;

import io.mango.common.exception.BizException;
import io.mango.infra.kv.api.IKvStore;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTicketServiceTest {

    @Test
    void consumeReturnsPayloadAndDeletesTicketFromKv() {
        InMemoryKvStore kvStore = new InMemoryKvStore();
        PasswordResetTicketService service = new PasswordResetTicketService(kvStore);
        PasswordResetTicketService.TicketPayload payload = new PasswordResetTicketService.TicketPayload(
                1001L, "1", "default", "internal-admin",
                "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 2001L);

        String ticket = service.issue(payload);
        PasswordResetTicketService.TicketPayload consumed = service.consume(ticket);

        assertThat(consumed).isEqualTo(payload);
        assertThat(kvStore.exists("auth:password-reset-ticket:" + ticket)).isFalse();
        assertThatThrownBy(() -> service.consume(ticket))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("强制改密凭据无效或已过期");
    }

    @Test
    void peekReturnsPayloadWithoutDeletingTicketFromKv() {
        InMemoryKvStore kvStore = new InMemoryKvStore();
        PasswordResetTicketService service = new PasswordResetTicketService(kvStore);
        PasswordResetTicketService.TicketPayload payload = new PasswordResetTicketService.TicketPayload(
                1001L, "1", "default", "internal-admin",
                "INTERNAL", "INTERNAL_USER", "INTERNAL_ORG", 2001L);

        String ticket = service.issue(payload);
        PasswordResetTicketService.TicketPayload peeked = service.peek(ticket);

        assertThat(peeked).isEqualTo(payload);
        assertThat(kvStore.exists("auth:password-reset-ticket:" + ticket)).isTrue();

        service.revoke(ticket);
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
