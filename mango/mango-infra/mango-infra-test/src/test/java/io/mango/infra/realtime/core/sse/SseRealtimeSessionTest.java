package io.mango.infra.realtime.core.sse;

import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimeProtocols;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SseRealtimeSession.
 */
class SseRealtimeSessionTest {

    @Test
    void protocol_returnsSSE() {
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, new SseEmitter(60_000L), () -> {});
        assertEquals(RealtimeProtocols.SSE, session.protocol());
    }

    @Test
    void isOpen_trueWhenEmitterNotNull() {
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, new SseEmitter(60_000L), () -> {});
        assertTrue(session.isOpen());
    }

    @Test
    void isOpen_falseWhenEmitterIsNull() {
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, null, () -> {});
        assertFalse(session.isOpen());
    }

    @Test
    void id_returnsSessionId() {
        SseRealtimeSession session = new SseRealtimeSession(
                "my-session-id", "tenant-a", 1L, new SseEmitter(60_000L), () -> {});
        assertEquals("my-session-id", session.id());
    }

    @Test
    void tenantId_and_userId_passThrough() {
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-xyz", 42L, new SseEmitter(60_000L), () -> {});
        assertEquals("tenant-xyz", session.tenantId());
        assertEquals(42L, session.userId());
    }

    @Test
    void closeCallback_isInvokedOnSendFailure() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, new FailingSseEmitter(), () -> closed.set(true));

        session.send(RealtimeMessage.of("type", "content"));

        assertTrue(closed.get());
        assertFalse(session.isOpen());
    }

    @Test
    void isOpen_falseAfterEmitterLifecycleCompletion() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        AtomicBoolean closed = new AtomicBoolean(false);
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, emitter, () -> closed.set(true));

        emitter.completeCallback.run();

        assertFalse(session.isOpen());
        assertTrue(closed.get());
    }

    @Test
    void lifecycleCallback_isIdempotent() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        AtomicBoolean closed = new AtomicBoolean(false);
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, emitter, () -> closed.set(!closed.get()));

        emitter.completeCallback.run();
        emitter.timeoutCallback.run();

        assertFalse(session.isOpen());
        assertTrue(closed.get());
    }

    @Test
    void isOpen_falseAfterEmitterLifecycleError() {
        CapturingSseEmitter emitter = new CapturingSseEmitter();
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, emitter, () -> {});

        emitter.triggerError();

        assertFalse(session.isOpen());
    }

    @Test
    void send_nullContent_doesNotThrow() throws Exception {
        SseEmitter emitter = new SseEmitter(60_000L);
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, emitter, () -> {});

        // Should not throw even with null content
        session.send(RealtimeMessage.of("notice", null));
    }

    @Test
    void send_fullMessage_doesNotThrow() throws Exception {
        SseEmitter emitter = new SseEmitter(60_000L);
        SseRealtimeSession session = new SseRealtimeSession(
                "id1", "tenant-a", 1L, emitter, () -> {});

        RealtimeMessage msg = RealtimeMessage.toUser(1L, "notice", "hello world");
        session.send(msg); // should not throw with valid emitter
    }

    private static class FailingSseEmitter extends SseEmitter {

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            throw new IOException("send failed");
        }
    }

    private static class CapturingSseEmitter extends SseEmitter {

        private Runnable completeCallback = () -> {};
        private Runnable timeoutCallback = () -> {};
        private Consumer<Throwable> errorCallback = error -> {};

        @Override
        public synchronized void onCompletion(Runnable callback) {
            this.completeCallback = callback;
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            this.timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(Consumer<Throwable> callback) {
            this.errorCallback = callback;
        }

        private void triggerError() {
            errorCallback.accept(new IllegalStateException("closed"));
        }
    }
}
