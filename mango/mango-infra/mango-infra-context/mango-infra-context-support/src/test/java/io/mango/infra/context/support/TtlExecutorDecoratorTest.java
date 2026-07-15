package io.mango.infra.context.support;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TtlExecutorDecoratorTest {

    private final TtlExecutorDecorator decorator = new TtlExecutorDecorator();

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void executorService_propagatesCurrentContextPerSubmissionWithoutLeakage() throws Exception {
        ExecutorService raw = Executors.newSingleThreadExecutor();
        ExecutorService executor = decorator.decorate(raw);
        try {
            setContext("request-1", "token-1");
            assertEquals("request-1:token-1", executor.submit(this::currentValues).get(2, TimeUnit.SECONDS));

            setContext("request-2", "token-2");
            assertEquals("request-2:token-2", executor.submit(this::currentValues).get(2, TimeUnit.SECONDS));

            MangoContextHolder.clear();
            assertEquals("null:null", executor.submit(this::currentValues).get(2, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        assertTrue(raw.isShutdown());
    }

    @Test
    void scheduledExecutorService_propagatesContextToDelayedCallable() throws Exception {
        ScheduledExecutorService executor = decorator.decorate(Executors.newSingleThreadScheduledExecutor());
        try {
            setContext("request-scheduled", "token-scheduled");
            String values = executor.schedule(this::currentValues, 10, TimeUnit.MILLISECONDS)
                    .get(2, TimeUnit.SECONDS);

            assertEquals("request-scheduled:token-scheduled", values);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void runnableAndCallable_captureContextAtDecorationTimeAndRestoreCaller() throws Exception {
        setContext("request-captured", "token-captured");
        AtomicReference<String> runnableValues = new AtomicReference<>();
        Runnable runnable = decorator.decorate(() -> runnableValues.set(currentValues()));
        Callable<String> callable = decorator.decorate(this::currentValues);

        setContext("request-current", "token-current");
        runnable.run();
        String callableValues = callable.call();

        assertEquals("request-captured:token-captured", runnableValues.get());
        assertEquals("request-captured:token-captured", callableValues);
        assertEquals("request-current:token-current", currentValues());
    }

    @Test
    void genericExecutor_propagatesContext() {
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            ExecutorService raw = Executors.newSingleThreadExecutor();
            try {
                Executor executor = decorator.decorate((Executor) raw);
                AtomicReference<String> values = new AtomicReference<>();
                setContext("request-generic", "token-generic");
                executor.execute(() -> values.set(currentValues()));
                raw.shutdown();
                raw.awaitTermination(2, TimeUnit.SECONDS);
                assertEquals("request-generic:token-generic", values.get());
            } finally {
                raw.shutdownNow();
            }
        });
    }

    private void setContext(String requestId, String token) {
        MangoContextHolder.set(MangoContextSnapshot.request(requestId, null, null, null, null));
        MangoContextHolder.setToken(token);
    }

    private String currentValues() {
        return MangoContextHolder.requestId() + ":" + MangoContextHolder.token();
    }
}
