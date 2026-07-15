package io.mango.infra.realtime.core.inbound.forward;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeTarget;
import io.mango.infra.realtime.core.session.RealtimeSession;
import io.mango.infra.realtime.core.session.RealtimeSubscriptionManager;
import io.mango.infra.realtime.core.polling.InMemoryRealtimePollingService;
import io.mango.infra.realtime.core.polling.RealtimeGroupSubscriptionKey;

import java.util.Objects;

/**
 * Restricts automatic client target publishing to the authenticated source's own scope.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Realtime services are injected singleton collaborators, not owned mutable state")
public class DefaultRealtimeInboundTargetAuthorizer implements RealtimeInboundTargetAuthorizer {

    private final RealtimeSubscriptionManager subscriptionManager;
    private final InMemoryRealtimePollingService pollingService;

    public DefaultRealtimeInboundTargetAuthorizer(RealtimeSubscriptionManager subscriptionManager) {
        this(Objects.requireNonNull(subscriptionManager, "subscriptionManager must not be null"), null);
    }

    public DefaultRealtimeInboundTargetAuthorizer(RealtimeSubscriptionManager subscriptionManager,
                                                  InMemoryRealtimePollingService pollingService) {
        this.subscriptionManager = subscriptionManager;
        this.pollingService = pollingService;
    }

    @Override
    public boolean canPublish(RealtimeInboundMessage message) {
        if (message == null || message.target() == null) {
            return false;
        }
        RealtimeTarget target = message.resolvedTarget();
        return switch (target.type()) {
            case USER -> ownsUser(message, target.id());
            case CLIENT -> ownsClient(message, target.id());
            case CONNECTION -> ownsConnection(message, target.id());
            case GROUP -> joinedGroup(message, target.id());
            case TENANT, BROADCAST -> false;
        };
    }

    private boolean ownsUser(RealtimeInboundMessage message, String targetId) {
        return message.userId() != null && String.valueOf(message.userId()).equals(targetId);
    }

    private boolean ownsClient(RealtimeInboundMessage message, String targetId) {
        String clientId = message.source().clientId();
        return clientId != null && !clientId.isBlank() && clientId.equals(targetId);
    }

    private boolean ownsConnection(RealtimeInboundMessage message, String targetId) {
        String sessionId = message.sessionId();
        return sessionId != null && !sessionId.isBlank() && sessionId.equals(targetId);
    }

    private boolean joinedGroup(RealtimeInboundMessage message, String groupId) {
        String sessionId = message.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        boolean connectionMember = subscriptionManager != null
                && subscriptionManager.findByGroup(message.tenantId(), groupId).stream()
                        .map(RealtimeSession::id)
                        .anyMatch(sessionId::equals);
        return connectionMember || pollingService != null
                && pollingService.isSubscribedToGroup(
                        new RealtimeGroupSubscriptionKey(sessionId, message.tenantId(), groupId));
    }
}
