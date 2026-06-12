package io.mango.infra.event.starter;

import io.mango.common.result.Require;
import io.mango.infra.event.core.transport.DomainEventTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * In-process scheduler for consuming transported domain events.
 */
public class DomainEventTransportScheduler implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainEventTransportScheduler.class);

    private final DomainEventTransport transport;
    private final ScheduledFuture<?> future;

    public DomainEventTransportScheduler(
            DomainEventTransport transport,
            TaskScheduler taskScheduler,
            long intervalMillis,
            long initialDelayMillis) {
        Require.notNull(transport, "事件传输不能为空");
        Require.notNull(taskScheduler, "任务调度器不能为空");
        Require.isTrue(intervalMillis > 0, "事件消费间隔必须大于 0");
        Require.isTrue(initialDelayMillis >= 0, "事件消费初始延迟不能为负数");
        this.transport = transport;
        PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(intervalMillis));
        trigger.setInitialDelay(Duration.ofMillis(initialDelayMillis));
        this.future = taskScheduler.schedule(this::consumeSafely, trigger);
    }

    public int consumeOnce() {
        return transport.consumeOnce();
    }

    @Override
    public void close() {
        if (future != null) {
            future.cancel(false);
        }
    }

    private void consumeSafely() {
        try {
            int count = transport.consumeOnce();
            if (count > 0) {
                LOGGER.debug("Domain event transport consumed: count={}", count);
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Domain event transport consume failed", ex);
        }
    }
}
