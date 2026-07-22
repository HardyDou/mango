package io.mango.resource.sync.starter;

import io.mango.resource.support.sync.StartupReadinessChangedEvent;
import io.mango.resource.support.sync.StartupReadinessState;
import io.mango.resource.support.sync.StartupReadinessStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Resource startup readiness tests")
class ResourceStartupReadinessTest {

    @Test
    @DisplayName("health should remain out of service until every startup participant is ready")
    void healthShouldAggregateParticipants() {
        MutableStatus resource = new MutableStatus("resource", StartupReadinessState.READY);
        MutableStatus tenants = new MutableStatus("tenants", StartupReadinessState.RECONCILING_TENANTS);
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("resourceStatus", resource);
        beans.registerSingleton("tenantStatus", tenants);
        ResourceStartupHealthIndicator indicator = new ResourceStartupHealthIndicator(
                beans.getBeanProvider(StartupReadinessStatus.class));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(indicator.health().getDetails()).containsKey("participants");
        assertThat(indicator.health().getDetails()).containsKey("participantDetails");

        tenants.state = StartupReadinessState.READY;

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("Boot accepting event should be reasserted as refusing until every participant converges")
    void bootAcceptingEventShouldNotOverrideParticipantReadiness() {
        MutableStatus resource = new MutableStatus("resource", StartupReadinessState.TRANSIENT_WAIT);
        GenericApplicationContext context = new GenericApplicationContext();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
        context.getBeanFactory().registerSingleton("resourceStatus", resource);
        ApplicationAvailabilityBean availability = new ApplicationAvailabilityBean();
        context.addApplicationListener(availability);
        List<ReadinessState> changes = new CopyOnWriteArrayList<>();
        context.addApplicationListener(event -> {
            if (event instanceof AvailabilityChangeEvent<?> availabilityEvent
                    && availabilityEvent.getState() instanceof ReadinessState readiness) {
                changes.add(readiness);
            }
        });
        context.registerBean(ResourceStartupReadinessCoordinator.class,
                () -> new ResourceStartupReadinessCoordinator(
                        context.getBeanProvider(StartupReadinessStatus.class), context, availability));
        context.refresh();
        ResourceStartupReadinessCoordinator coordinator = context.getBean(ResourceStartupReadinessCoordinator.class);

        coordinator.onStartupReadinessChanged(
                new StartupReadinessChangedEvent("resource", StartupReadinessState.TRANSIENT_WAIT));
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);

        awaitReadinessState(availability, ReadinessState.REFUSING_TRAFFIC);
        assertThat(availability.getReadinessState()).as("readiness changes: %s", changes)
                .isEqualTo(ReadinessState.REFUSING_TRAFFIC);

        resource.state = StartupReadinessState.READY;
        coordinator.onStartupReadinessChanged(
                new StartupReadinessChangedEvent("resource", StartupReadinessState.READY));

        assertThat(changes).containsExactly(
                ReadinessState.REFUSING_TRAFFIC,
                ReadinessState.ACCEPTING_TRAFFIC,
                ReadinessState.REFUSING_TRAFFIC,
                ReadinessState.ACCEPTING_TRAFFIC);
        assertThat(availability.getReadinessState()).isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
        context.close();
    }

    private void awaitReadinessState(ApplicationAvailabilityBean availability, ReadinessState expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (availability.getReadinessState() != expected && System.nanoTime() < deadline) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }

    private static final class MutableStatus implements StartupReadinessStatus {

        private final String component;
        private StartupReadinessState state;

        private MutableStatus(String component, StartupReadinessState state) {
            this.component = component;
            this.state = state;
        }

        @Override
        public String getReadinessComponent() {
            return component;
        }

        @Override
        public StartupReadinessState getReadinessState() {
            return state;
        }
    }
}
