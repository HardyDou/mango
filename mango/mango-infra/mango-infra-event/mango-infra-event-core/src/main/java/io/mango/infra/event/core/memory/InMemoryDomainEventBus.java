package io.mango.infra.event.core.memory;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.api.DomainEventHandler;
import io.mango.infra.event.api.IDomainEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进程内事件总线。
 */
public class InMemoryDomainEventBus implements IDomainEventBus {

    public static final String WILDCARD = "*";

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryDomainEventBus.class);

    private final Map<String, CopyOnWriteArrayList<DomainEventHandler>> handlers = new ConcurrentHashMap<>();

    @Override
    public void publish(DomainEvent event) {
        if (event == null || isBlank(event.getEventType())) {
            throw new IllegalArgumentException("event and eventType must not be blank");
        }
        dispatch(event.getEventType(), event);
        dispatch(WILDCARD, event);
    }

    @Override
    public AutoCloseable subscribe(String eventType, DomainEventHandler handler) {
        if (isBlank(eventType)) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        String key = eventType.trim();
        handlers.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(handler);
        return () -> unsubscribe(key, handler);
    }

    private void dispatch(String eventType, DomainEvent event) {
        List<DomainEventHandler> eventHandlers = handlers.get(eventType);
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            return;
        }
        for (DomainEventHandler handler : eventHandlers) {
            try {
                handler.handle(event);
            } catch (RuntimeException ex) {
                LOGGER.error("Domain event handler failed. eventType={}, eventId={}", event.getEventType(), event.getEventId(), ex);
            }
        }
    }

    private void unsubscribe(String eventType, DomainEventHandler handler) {
        CopyOnWriteArrayList<DomainEventHandler> eventHandlers = handlers.get(eventType);
        if (eventHandlers == null) {
            return;
        }
        eventHandlers.remove(handler);
        if (eventHandlers.isEmpty()) {
            handlers.remove(eventType, eventHandlers);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
