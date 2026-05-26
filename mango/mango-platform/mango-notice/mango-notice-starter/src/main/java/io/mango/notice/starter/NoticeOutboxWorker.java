package io.mango.notice.starter;

import io.mango.infra.kv.api.IOutboxDispatcher;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NoticeOutboxWorker implements AutoCloseable {

    private final IOutboxDispatcher dispatcher;
    private final ScheduledExecutorService executor;

    public NoticeOutboxWorker(IOutboxDispatcher dispatcher,
                              String workerId,
                              long initialDelayMillis,
                              long fixedDelayMillis) {
        this.dispatcher = dispatcher;
        String safeWorkerId = workerId == null || workerId.isBlank() ? "notice-outbox-worker" : workerId.trim();
        long safeInitialDelayMillis = Math.max(0L, initialDelayMillis);
        long safeFixedDelayMillis = fixedDelayMillis <= 0L ? 1000L : fixedDelayMillis;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "notice-outbox-worker-" + safeWorkerId);
            thread.setDaemon(true);
            return thread;
        });
        this.executor.scheduleWithFixedDelay(this::dispatchSafely,
                safeInitialDelayMillis,
                safeFixedDelayMillis,
                TimeUnit.MILLISECONDS);
    }

    public int dispatchOnce() {
        return dispatcher.dispatchOnce();
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    private void dispatchSafely() {
        try {
            int count = dispatcher.dispatchOnce();
            if (count > 0) {
                log.debug("Notice outbox dispatched: count={}", count);
            }
        } catch (RuntimeException ex) {
            log.warn("Notice outbox dispatch failed", ex);
        }
    }
}
