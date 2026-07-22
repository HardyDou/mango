package io.mango.resource.sync.starter;

import io.mango.resource.support.sync.StartupReadinessChangedEvent;
import io.mango.resource.support.sync.StartupReadinessStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Bridges resource startup participants to Spring Boot readiness without changing liveness.
 */
@Slf4j
public class ResourceStartupReadinessCoordinator implements AutoCloseable {

    private final ObjectProvider<StartupReadinessStatus> statuses;
    private final ApplicationContext applicationContext;
    private final ApplicationAvailability applicationAvailability;
    private final ExecutorService reconciliationExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "mango-readiness-reconciliation");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean acceptingTrafficObserved = new AtomicBoolean();
    private final AtomicBoolean reconciliationScheduled = new AtomicBoolean();
    private final AtomicReference<ReadinessState> publishedState = new AtomicReference<>();

    public ResourceStartupReadinessCoordinator(ObjectProvider<StartupReadinessStatus> statuses,
                                               ApplicationContext applicationContext,
                                               ApplicationAvailability applicationAvailability) {
        this.statuses = statuses;
        this.applicationContext = applicationContext;
        this.applicationAvailability = applicationAvailability;
    }

    /**
     * Reconciles Spring Boot's own readiness publication with Mango startup participants.
     * Spring Boot publishes ACCEPTING_TRAFFIC after ApplicationReadyEvent, so reacting to
     * ApplicationReadyEvent itself would be overwritten immediately afterwards.
     *
     * @param event availability state change
     */
    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onAvailabilityChanged(AvailabilityChangeEvent<?> event) {
        if (!(event.getState() instanceof ReadinessState readinessState)) {
            return;
        }
        publishedState.set(readinessState);
        if (readinessState == ReadinessState.ACCEPTING_TRAFFIC) {
            acceptingTrafficObserved.set(true);
            reconcileAfterCurrentEvent();
        }
    }

    /**
     * Re-evaluates readiness when resource synchronization or tenant reconciliation changes state.
     *
     * @param event participant state change
     */
    @EventListener
    public void onStartupReadinessChanged(StartupReadinessChangedEvent event) {
        evaluate();
    }

    void evaluate() {
        List<StartupReadinessStatus> participants = statuses.orderedStream().toList();
        if (participants.isEmpty()) {
            return;
        }
        boolean allReady = participants.stream().allMatch(StartupReadinessStatus::isReady);
        if (allReady && !acceptingTrafficObserved.get()) {
            return;
        }
        ReadinessState nextState = allReady
                ? ReadinessState.ACCEPTING_TRAFFIC : ReadinessState.REFUSING_TRAFFIC;
        ReadinessState previous = publishedState.getAndSet(nextState);
        if (previous == nextState) {
            return;
        }
        AvailabilityChangeEvent.publish(applicationContext, nextState);
        log.info("Mango startup readiness changed: state={}, participants={}", nextState,
                participants.stream()
                        .map(status -> status.getReadinessComponent() + '=' + status.getReadinessState())
                        .toList());
    }

    private void reconcileAfterCurrentEvent() {
        if (!reconciliationScheduled.compareAndSet(false, true)) {
            return;
        }
        reconciliationExecutor.execute(() -> {
            try {
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
                while (applicationAvailability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC
                        && System.nanoTime() < deadline) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                }
                evaluate();
            } finally {
                reconciliationScheduled.set(false);
            }
        });
    }

    @Override
    public void close() {
        reconciliationExecutor.shutdownNow();
    }
}
