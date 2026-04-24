package io.mango.infra.realtime.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.core.redis.RedisKvStore;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.IRealtimeOutboundForwardService;
import io.mango.infra.realtime.core.outbound.RealtimeProtocolSender;
import io.mango.infra.realtime.core.outbound.RealtimePublishService;
import io.mango.infra.realtime.core.presence.RealtimeNode;
import io.mango.infra.realtime.core.presence.RealtimePresence;
import io.mango.infra.realtime.starter.presence.KvRealtimePresenceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Redis integration tests for realtime presence routing.
 * Requires Redis running on localhost:6379 with no password.
 */
class RedisKvRealtimePresenceIntegrationTest {

    private static final String KEY_PREFIX = "rt:presence:" + System.currentTimeMillis();

    private static RedissonClient redisson;
    private static IKvStore kvStore;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void beforeAll() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setDatabase(0)
                .setConnectTimeout(5000)
                .setTimeout(3000)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(8);
        redisson = Redisson.create(config);
        kvStore = new RedisKvStore(redisson);
        objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (redisson != null) {
            redisson.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        redisson.getKeys().deleteByPattern(KEY_PREFIX + "*");
    }

    @Test
    void onlineFindAndOffline_managePresenceRoutesInRedis() throws Exception {
        try (KvRealtimePresenceService presenceService = new KvRealtimePresenceService(
                kvStore,
                (RedisKvStore) kvStore,
                objectMapper,
                KEY_PREFIX,
                30)) {
            RealtimePresence wsA = RealtimePresence.of(
                    "session-a",
                    "tenant-a",
                    1001L,
                    "websocket",
                    new RealtimeNode("node-a", "svc-a", "/", "/_realtime/messages/outbound"));
            RealtimePresence sseB = RealtimePresence.of(
                    "session-b",
                    "tenant-a",
                    1002L,
                    "sse",
                    new RealtimeNode("node-b", "svc-b", "/app", "/_realtime/messages/outbound"));

            presenceService.online(wsA);
            presenceService.online(sseB);

            assertThat(presenceService.findByUser(1001L))
                    .extracting(RealtimePresence::sessionId)
                    .containsExactly("session-a");
            assertThat(presenceService.findByTenant("tenant-a"))
                    .extracting(RealtimePresence::sessionId)
                    .containsExactlyInAnyOrder("session-a", "session-b");
            assertThat(presenceService.findAll())
                    .extracting(RealtimePresence::routeKey)
                    .containsExactlyInAnyOrder(wsA.routeKey(), sseB.routeKey());

            presenceService.offline("session-a");

            assertThat(presenceService.findByUser(1001L)).isEmpty();
            assertThat(presenceService.findByTenant("tenant-a"))
                    .extracting(RealtimePresence::sessionId)
                    .containsExactly("session-b");
        }
    }

    @Test
    void refresher_keepsLocalPresenceAliveBeforeTtlExpires() throws Exception {
        try (KvRealtimePresenceService presenceService = new KvRealtimePresenceService(
                kvStore,
                (RedisKvStore) kvStore,
                objectMapper,
                KEY_PREFIX,
                9)) {
            RealtimePresence presence = RealtimePresence.of(
                    "session-refresh",
                    "tenant-refresh",
                    3001L,
                    "websocket",
                    new RealtimeNode("node-refresh", "svc-refresh", "/", "/_realtime/messages/outbound"));

            presenceService.online(presence);
            Thread.sleep(Duration.ofSeconds(6).toMillis());

            assertThat(presenceService.findByUser(3001L))
                    .extracting(RealtimePresence::sessionId)
                    .containsExactly("session-refresh");
        }
    }

    @Test
    void publish_usesRedisPresenceToForwardOnlyRemoteNodes() throws Exception {
        try (KvRealtimePresenceService presenceService = new KvRealtimePresenceService(
                kvStore,
                (RedisKvStore) kvStore,
                objectMapper,
                KEY_PREFIX,
                30)) {
            RealtimeNode localNode = new RealtimeNode("node-local", "svc-local", "/", "/_realtime/messages/outbound");
            RealtimePresence localPresence = RealtimePresence.of(
                    "session-local",
                    "tenant-a",
                    2001L,
                    "websocket",
                    localNode);
            RealtimePresence remotePresence = RealtimePresence.of(
                    "session-remote",
                    "tenant-a",
                    2001L,
                    "sse",
                    new RealtimeNode("node-remote", "svc-remote", "/rt", "/_realtime/messages/outbound"));

            presenceService.online(localPresence);
            presenceService.online(remotePresence);

            RecordingProtocolSender protocolSender = new RecordingProtocolSender();
            RecordingOutboundForwardService forwardService = new RecordingOutboundForwardService();
            RealtimePublishService publishService = new RealtimePublishService(
                    List.of(protocolSender),
                    presenceService,
                    forwardService,
                    localNode);

            RealtimeOutboundMessage message = RealtimeOutboundMessage.toUser(2001L, "task.done", "ok");
            publishService.publish(message);

            assertThat(protocolSender.userMessages)
                    .extracting(RealtimeOutboundMessage::type)
                    .containsExactly("task.done");
            assertThat(forwardService.forwardedMessages)
                    .extracting(RealtimeOutboundMessage::type)
                    .containsExactly("task.done");
            assertThat(forwardService.forwardedPresences)
                    .extracting(RealtimePresence::instanceId)
                    .containsExactly("node-remote");
        }
    }

    private static final class RecordingProtocolSender implements RealtimeProtocolSender {

        private final List<RealtimeOutboundMessage> userMessages = new ArrayList<>();

        @Override
        public String protocol() {
            return "recording";
        }

        @Override
        public void sendToUser(Long userId, RealtimeOutboundMessage envelope) {
            userMessages.add(envelope);
        }

        @Override
        public void sendToTenant(String tenantId, RealtimeOutboundMessage envelope) {
        }

        @Override
        public void broadcast(RealtimeOutboundMessage envelope) {
        }
    }

    private static final class RecordingOutboundForwardService implements IRealtimeOutboundForwardService {

        private final List<RealtimePresence> forwardedPresences = new ArrayList<>();
        private final List<RealtimeOutboundMessage> forwardedMessages = new ArrayList<>();

        @Override
        public void forward(Collection<RealtimePresence> presences, RealtimeOutboundMessage message) {
            forwardedPresences.addAll(presences);
            forwardedMessages.add(message);
        }
    }
}
