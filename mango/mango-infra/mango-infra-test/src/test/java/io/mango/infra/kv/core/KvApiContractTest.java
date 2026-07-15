package io.mango.infra.kv.core;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.OutboxMessage;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KvApiContractTest {

    @Test
    void outboxMessage_mapsAndBuilderResult_doNotExposeMutableState() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", 100);
        Map<String, String> headers = new HashMap<>();
        headers.put("trace", "trace-1");

        OutboxMessage.Builder builder = OutboxMessage.builder()
                .topic("payment")
                .payload(payload)
                .headers(headers);
        OutboxMessage first = builder.build();

        payload.put("amount", 200);
        headers.put("trace", "trace-2");
        first.getPayload().put("amount", 300);
        first.getHeaders().put("trace", "trace-3");
        builder.topic("notice");
        OutboxMessage second = builder.build();

        assertThat(first.getPayload()).containsEntry("amount", 100);
        assertThat(first.getHeaders()).containsEntry("trace", "trace-1");
        assertThat(first.getTopic()).isEqualTo("payment");
        assertThat(second.getTopic()).isEqualTo("notice");
    }

    @Test
    void defaultSet_whenStoreRejectsWrite_failsInsteadOfSilentlyLosingValue() {
        IKvStore rejectingStore = new RejectingKvStore();

        assertThatThrownBy(() -> rejectingStore.set("order:1", "paid", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("order:1");
    }

    private static final class RejectingKvStore implements IKvStore {

        @Override
        public boolean setIfAbsent(String key, String value, long expireSeconds) {
            return false;
        }

        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public void delete(String key) {
            // Nothing to delete in this contract fixture.
        }

        @Override
        public boolean exists(String key) {
            return false;
        }
    }
}
