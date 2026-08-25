package io.mango.ai.starter.controller;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 将内部 JSON 事件流适配为异步 SSE 响应。
 */
final class AiSseEmitterFactory {

    private static final long CHAT_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private AiSseEmitterFactory() {
    }

    static SseEmitter create(Flux<String> events) {
        EmitterBridge bridge = new EmitterBridge(CHAT_TIMEOUT_MILLIS);
        Disposable subscription = events.publishOn(Schedulers.boundedElastic())
                .subscribe(bridge::send, bridge::fail, bridge::complete);
        bridge.attach(subscription);
        return bridge.emitter();
    }

    private static final class EmitterBridge {

        private final SseEmitter emitter;
        private final AtomicReference<Disposable> subscription = new AtomicReference<>();
        private final AtomicBoolean terminated = new AtomicBoolean();

        private EmitterBridge(long timeout) {
            emitter = new SseEmitter(timeout);
            emitter.onCompletion(this::cancel);
            emitter.onTimeout(this::cancel);
            emitter.onError(error -> cancel());
        }

        private SseEmitter emitter() {
            return emitter;
        }

        private void attach(Disposable disposable) {
            subscription.set(disposable);
            if (terminated.get()) {
                disposable.dispose();
            }
        }

        private void send(String event) {
            if (terminated.get()) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException exception) {
                fail(exception);
            }
        }

        private void fail(Throwable error) {
            if (!terminated.compareAndSet(false, true)) {
                return;
            }
            emitter.completeWithError(error);
            disposeSubscription();
        }

        private void complete() {
            if (terminated.compareAndSet(false, true)) {
                emitter.complete();
                disposeSubscription();
            }
        }

        private void cancel() {
            terminated.set(true);
            disposeSubscription();
        }

        private void disposeSubscription() {
            Disposable disposable = subscription.get();
            if (disposable != null) {
                disposable.dispose();
            }
        }
    }
}
