package io.mango.infra.realtime.support.inbound;

import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RealtimeInboundService implements IRealtimeInboundService {

    private static final Logger log = Logger.getLogger(RealtimeInboundService.class.getName());

    private final ListableBeanFactory beanFactory;
    private final boolean failFast;
    private final RealtimeInboundUnknownTypePolicy unknownTypePolicy;
    private final RealtimeInboundListenerScanner listenerScanner;
    private volatile Map<String, List<RealtimeInboundListenerInvoker>> listenersByType;

    public RealtimeInboundService(ListableBeanFactory beanFactory,
                                  boolean failFast,
                                  RealtimeInboundUnknownTypePolicy unknownTypePolicy) {
        this.beanFactory = beanFactory;
        this.failFast = failFast;
        this.unknownTypePolicy = unknownTypePolicy == null
                ? RealtimeInboundUnknownTypePolicy.IGNORE
                : unknownTypePolicy;
        this.listenerScanner = new RealtimeInboundListenerScanner();
    }

    @Override
    public void dispatch(RealtimeInboundMessage message) {
        if (message == null) {
            return;
        }
        List<RealtimeInboundListenerInvoker> listeners = listenersByType().getOrDefault(message.type(), List.of());
        if (listeners.isEmpty()) {
            handleUnknownType(message);
            return;
        }
        for (RealtimeInboundListenerInvoker listener : listeners) {
            try {
                listener.invoke(message);
            } catch (Exception e) {
                log.log(Level.WARNING,
                        "Failed to dispatch realtime inbound message " + message.id()
                                + " to listener " + listener.description(),
                        e);
                if (failFast) {
                    throw e instanceof RuntimeException runtimeException
                            ? runtimeException
                            : new IllegalStateException("Failed to dispatch realtime inbound message", e);
                }
            }
        }
    }

    @Override
    public boolean hasListeners() {
        return !listenersByType().isEmpty();
    }

    private Map<String, List<RealtimeInboundListenerInvoker>> listenersByType() {
        Map<String, List<RealtimeInboundListenerInvoker>> listeners = listenersByType;
        if (listeners == null) {
            synchronized (this) {
                listeners = listenersByType;
                if (listeners == null) {
                    listeners = listenerScanner.scan(beanFactory);
                    listenersByType = listeners;
                }
            }
        }
        return listeners;
    }

    private void handleUnknownType(RealtimeInboundMessage message) {
        if (unknownTypePolicy == RealtimeInboundUnknownTypePolicy.WARN) {
            log.warning("No realtime listener found for inbound type " + message.type());
        }
        if (unknownTypePolicy == RealtimeInboundUnknownTypePolicy.ERROR) {
            throw new IllegalStateException("No realtime listener found for inbound type " + message.type());
        }
    }
}
