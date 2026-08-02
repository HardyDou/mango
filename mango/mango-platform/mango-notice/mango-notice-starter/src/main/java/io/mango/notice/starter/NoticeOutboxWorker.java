package io.mango.notice.starter;

import io.mango.infra.kv.api.IOutboxDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NoticeOutboxWorker implements AutoCloseable {

    private final IOutboxDispatcher dispatcher;
    private final ScheduledExecutorService executor;
    private final long initialDelayMillis;
    private final long fixedDelayMillis;
    private boolean started;
    private boolean closed;

    public NoticeOutboxWorker(IOutboxDispatcher dispatcher,
                              String workerId,
                              long initialDelayMillis,
                              long fixedDelayMillis) {
        this.dispatcher = dispatcher;
        String safeWorkerId = workerId == null || workerId.isBlank() ? "notice-outbox-worker" : workerId.trim();
        this.initialDelayMillis = Math.max(0L, initialDelayMillis);
        this.fixedDelayMillis = fixedDelayMillis <= 0L ? 1000L : fixedDelayMillis;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "notice-outbox-worker-" + safeWorkerId);
            thread.setDaemon(true);
            return thread;
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void startOnReady() {
        if (started || closed) {
            return;
        }
        started = true;
        executor.scheduleWithFixedDelay(this::dispatchSafely,
                initialDelayMillis, fixedDelayMillis, TimeUnit.MILLISECONDS);
    }

    public int dispatchOnce() {
        return dispatcher.dispatchOnce();
    }

    @Override
    public synchronized void close() {
        closed = true;
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
