package io.mango.infra.context.support;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MangoContextTaskDecoratorTest {

    @AfterEach
    void clearContext() {
        MangoContextHolder.clear();
    }

    @Test
    void decorate_capturesSubmissionContextAndToken() {
        MangoContextHolder.set(MangoContextSnapshot.request("request-captured", null, null, null, null));
        MangoContextHolder.setToken("token-captured");
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();

        Runnable decorated = new MangoContextTaskDecorator().decorate(() -> {
            requestId.set(MangoContextHolder.requestId());
            token.set(MangoContextHolder.token());
        });
        MangoContextHolder.set(MangoContextSnapshot.request("request-later", null, null, null, null));
        MangoContextHolder.setToken("token-later");
        decorated.run();

        assertEquals("request-captured", requestId.get());
        assertEquals("token-captured", token.get());
        assertEquals("request-later", MangoContextHolder.requestId());
        assertEquals("token-later", MangoContextHolder.token());
    }

    @Test
    void decoratedTask_restoresEmptyWorkerContextAfterCompletion() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.request("request-captured", null, null, null, null));
        MangoContextHolder.setToken("token-captured");
        Runnable decorated = new MangoContextTaskDecorator().decorate(() -> {
            assertEquals("request-captured", MangoContextHolder.requestId());
            assertEquals("token-captured", MangoContextHolder.token());
        });
        MangoContextHolder.clear();

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            worker.submit(decorated).get(2, TimeUnit.SECONDS);
            assertNull(worker.submit(MangoContextHolder::requestId).get(2, TimeUnit.SECONDS));
            assertNull(worker.submit(MangoContextHolder::token).get(2, TimeUnit.SECONDS));
        } finally {
            worker.shutdownNow();
        }
    }
}
