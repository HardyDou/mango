package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeContext;
import io.mango.infra.realtime.api.dto.RealtimeEvent;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePayload;
import io.mango.infra.realtime.api.dto.RealtimeSource;
import io.mango.infra.realtime.api.dto.RealtimeTarget;
import io.mango.infra.realtime.core.presence.InMemoryRealtimePresenceService;
import io.mango.infra.realtime.core.presence.RealtimeNode;
import io.mango.infra.realtime.core.session.InMemoryRealtimeSubscriptionManager;
import io.mango.infra.realtime.core.session.RealtimeSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultRealtimeInboundTargetAuthorizerTest {

    private InMemoryRealtimeSubscriptionManager subscriptions;
    private DefaultRealtimeInboundTargetAuthorizer authorizer;

    @BeforeEach
    void setUp() {
        subscriptions = new InMemoryRealtimeSubscriptionManager(
                new InMemoryRealtimePresenceService(),
                new RealtimeNode("node-a", "realtime", "/", "/_realtime/messages/outbound"));
        subscriptions.subscribe(new TestSession("session-a", "tenant-a", 1001L, "client-a"));
        subscriptions.subscribeGroup("session-a", "room-a");
        authorizer = new DefaultRealtimeInboundTargetAuthorizer(subscriptions);
    }

    @Test
    void allowsOnlyTargetsOwnedByAuthenticatedSource() {
        assertThat(authorizer.canPublish(message(RealtimeTarget.user(1001L)))).isTrue();
        assertThat(authorizer.canPublish(message(RealtimeTarget.client("client-a")))).isTrue();
        assertThat(authorizer.canPublish(message(RealtimeTarget.connection("session-a")))).isTrue();
        assertThat(authorizer.canPublish(message(RealtimeTarget.group("room-a")))).isTrue();
    }

    @Test
    void rejectsOtherUsersClientsConnectionsGroupsAndBroadTargets() {
        assertThat(authorizer.canPublish(message(RealtimeTarget.user(2002L)))).isFalse();
        assertThat(authorizer.canPublish(message(RealtimeTarget.client("client-b")))).isFalse();
        assertThat(authorizer.canPublish(message(RealtimeTarget.connection("session-b")))).isFalse();
        assertThat(authorizer.canPublish(message(RealtimeTarget.group("room-b")))).isFalse();
        assertThat(authorizer.canPublish(message(RealtimeTarget.tenant("tenant-a")))).isFalse();
        assertThat(authorizer.canPublish(message(RealtimeTarget.broadcast()))).isFalse();
    }

    private RealtimeInboundMessage message(RealtimeTarget target) {
        return new RealtimeInboundMessage(
                "m1", "1.0", RealtimeEvent.of("chat", "message.send"),
                new RealtimeSource("web", "client-a", "session-a"),
                RealtimeContext.of("tenant-a", 1001L), target, null,
                RealtimePayload.text("hello"), null, null, null, null);
    }

    private record TestSession(String id, String tenantId, Long userId, String clientId) implements RealtimeSession {
        @Override
        public String protocol() {
            return "TEST";
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void send(io.mango.infra.realtime.api.dto.RealtimeOutboundMessage envelope) {
        }
    }
}
