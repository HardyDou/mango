package io.mango.infra.realtime.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.realtime.core.presence.RealtimeNode;
import io.mango.infra.realtime.core.presence.RealtimePresence;
import io.mango.infra.realtime.starter.presence.KvRealtimePresenceService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class KvRealtimePresenceGroupIntegrationTest {

    private static final String KEY_PREFIX = "rt:presence:memory:" + System.currentTimeMillis();

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Test
    void groupPresence_refreshesGroupIndexAndCleansItOnOffline() throws Exception {
        try (MemoryKvStore memoryKvStore = new MemoryKvStore();
             KvRealtimePresenceService presenceService = new KvRealtimePresenceService(
                     memoryKvStore,
                     memoryKvStore,
                     objectMapper,
                     KEY_PREFIX,
                     9)) {
            RealtimePresence presence = RealtimePresence.of(
                    "session-group",
                    "tenant-group",
                    4001L,
                    "client-group",
                    "websocket",
                    new RealtimeNode("node-group", "svc-group", "/", "/_realtime/messages/outbound"));

            presenceService.online(presence);
            presenceService.joinGroup("session-group", "tenant-group", "room-001");

            Thread.sleep(Duration.ofSeconds(6).toMillis());

            assertThat(presenceService.findByGroup("tenant-group", "room-001"))
                    .extracting(RealtimePresence::sessionId)
                    .containsExactly("session-group");

            presenceService.offline("session-group");

            assertThat(presenceService.findByGroup("tenant-group", "room-001")).isEmpty();
        }
    }
}
