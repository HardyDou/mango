package io.mango.resource.sync.starter;

import io.mango.resource.support.sync.StartupReadinessStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports resource synchronization and dependent reconciliation startup states.
 */
public class ResourceStartupHealthIndicator implements HealthIndicator {

    private final ObjectProvider<StartupReadinessStatus> statuses;

    public ResourceStartupHealthIndicator(ObjectProvider<StartupReadinessStatus> statuses) {
        this.statuses = statuses;
    }

    @Override
    public Health health() {
        List<StartupReadinessStatus> participants = statuses.orderedStream().toList();
        Map<String, String> states = new LinkedHashMap<>();
        Map<String, Map<String, Object>> participantDetails = new LinkedHashMap<>();
        participants.forEach(status -> states.put(
                status.getReadinessComponent(), status.getReadinessState().name()));
        participants.forEach(status -> participantDetails.put(
                status.getReadinessComponent(), status.getReadinessDetails()));
        boolean ready = participants.stream().allMatch(StartupReadinessStatus::isReady);
        Health.Builder builder = ready ? Health.up() : Health.status(Status.OUT_OF_SERVICE);
        return builder.withDetail("participants", states)
                .withDetail("participantDetails", participantDetails)
                .build();
    }
}
