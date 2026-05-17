package io.mango.infra.event.starter;

import io.mango.common.result.Require;
import io.mango.infra.kv.api.IOutboxDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * In-process scheduler for domain event outbox dispatch.
 */
public class OutboxDispatchScheduler implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxDispatchScheduler.class);

    private final IOutboxDispatcher dispatcher;
    private final ScheduledFuture<?> future;

    public OutboxDispatchScheduler(
            IOutboxDispatcher dispatcher,
            TaskScheduler taskScheduler,
            long dispatchIntervalMillis,
            long dispatchInitialDelayMillis) {
        Require.notNull(dispatcher, "Outbox 分发器不能为空");
        Require.notNull(taskScheduler, "任务调度器不能为空");
        Require.isTrue(dispatchIntervalMillis > 0, "Outbox 分发间隔必须大于 0");
        Require.isTrue(dispatchInitialDelayMillis >= 0, "Outbox 初始延迟不能为负数");
        this.dispatcher = dispatcher;
        PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(dispatchIntervalMillis));
        trigger.setInitialDelay(Duration.ofMillis(dispatchInitialDelayMillis));
        this.future = taskScheduler.schedule(this::dispatchSafely, trigger);
    }

    public int dispatchOnce() {
        return dispatcher.dispatchOnce();
    }

    @Override
    public void close() {
        if (future != null) {
            future.cancel(false);
        }
    }

    private void dispatchSafely() {
        try {
            int count = dispatcher.dispatchOnce();
            if (count > 0) {
                LOGGER.debug("Domain event outbox dispatched: count={}", count);
            }
        } catch (RuntimeException ex) {
            LOGGER.warn("Domain event outbox dispatch failed", ex);
        }
    }
}
