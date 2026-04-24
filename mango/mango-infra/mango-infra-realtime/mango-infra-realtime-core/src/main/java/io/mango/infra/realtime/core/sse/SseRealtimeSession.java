package io.mango.infra.realtime.core.sse;

import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeProtocols;
import io.mango.infra.realtime.core.session.RealtimeSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SseRealtimeSession implements RealtimeSession {

    private final String id;
    private final String tenantId;
    private final Long userId;
    private final SseEmitter emitter;
    private final Runnable closeCallback;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public SseRealtimeSession(String id,
                              String tenantId,
                              Long userId,
                              SseEmitter emitter,
                              Runnable closeCallback) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.emitter = emitter;
        this.closeCallback = closeCallback == null ? () -> { } : closeCallback;
        registerLifecycleCallbacks();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String protocol() {
        return RealtimeProtocols.SSE;
    }

    @Override
    public String tenantId() {
        return tenantId;
    }

    @Override
    public Long userId() {
        return userId;
    }

    @Override
    public boolean isOpen() {
        return emitter != null && open.get();
    }

    @Override
    public void send(RealtimeOutboundMessage envelope) {
        if (!isOpen()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(envelope));
        } catch (IOException e) {
            close();
        }
    }

    private void registerLifecycleCallbacks() {
        if (emitter == null) {
            open.set(false);
            return;
        }
        emitter.onCompletion(this::close);
        emitter.onTimeout(this::close);
        emitter.onError(error -> close());
    }

    private void close() {
        if (open.compareAndSet(true, false)) {
            closeCallback.run();
        }
    }
}
