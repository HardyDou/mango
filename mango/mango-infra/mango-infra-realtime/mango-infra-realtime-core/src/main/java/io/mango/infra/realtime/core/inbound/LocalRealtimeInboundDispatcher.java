package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeListener;
import io.mango.infra.realtime.api.RealtimeSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LocalRealtimeInboundDispatcher implements RealtimeInboundDispatcher {

    private final ListableBeanFactory beanFactory;
    private final boolean failFast;
    private final InboundUnknownTypePolicy unknownTypePolicy;
    private volatile Map<String, List<RealtimeListenerInvoker>> listenersByType;

    public LocalRealtimeInboundDispatcher(ListableBeanFactory beanFactory,
                                          boolean failFast,
                                          InboundUnknownTypePolicy unknownTypePolicy) {
        this.beanFactory = beanFactory;
        this.failFast = failFast;
        this.unknownTypePolicy = unknownTypePolicy == null ? InboundUnknownTypePolicy.IGNORE : unknownTypePolicy;
    }

    @Override
    public void dispatch(RealtimeInboundMessage message) {
        if (message == null) {
            return;
        }
        List<RealtimeListenerInvoker> listeners = listenersByType().getOrDefault(message.type(), List.of());
        if (listeners.isEmpty()) {
            handleUnknownType(message);
            return;
        }
        for (RealtimeListenerInvoker listener : listeners) {
            try {
                listener.invoke(message);
            } catch (Exception e) {
                log.warn("Failed to dispatch realtime inbound message {} to listener {}",
                        message.id(), listener.description(), e);
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

    private Map<String, List<RealtimeListenerInvoker>> listenersByType() {
        Map<String, List<RealtimeListenerInvoker>> listeners = listenersByType;
        if (listeners == null) {
            synchronized (this) {
                listeners = listenersByType;
                if (listeners == null) {
                    listeners = scanListeners(beanFactory);
                    listenersByType = listeners;
                }
            }
        }
        return listeners;
    }

    private void handleUnknownType(RealtimeInboundMessage message) {
        if (unknownTypePolicy == InboundUnknownTypePolicy.WARN) {
            log.warn("No realtime listener found for inbound type {}", message.type());
        }
        if (unknownTypePolicy == InboundUnknownTypePolicy.ERROR) {
            throw new IllegalStateException("No realtime listener found for inbound type " + message.type());
        }
    }

    private Map<String, List<RealtimeListenerInvoker>> scanListeners(ListableBeanFactory beanFactory) {
        Map<String, List<RealtimeListenerInvoker>> listeners = new LinkedHashMap<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Object bean = beanFactory.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            List<RealtimeListenerInvoker> methodInvokers = methodInvokers(bean, targetClass);
            if (!methodInvokers.isEmpty()) {
                addInvokers(listeners, methodInvokers);
                continue;
            }
            RealtimeListener classListener = AnnotationUtils.findAnnotation(targetClass, RealtimeListener.class);
            if (classListener != null) {
                if (!(bean instanceof RealtimeSubscriber subscriber)) {
                    throw new IllegalStateException("@RealtimeListener class must implement RealtimeSubscriber: "
                            + targetClass.getName());
                }
                addInvoker(listeners, new SubscriberRealtimeListenerInvoker(subscriber, classListener, targetClass));
            }
        }
        listeners.values().forEach(invokers -> invokers.sort(Comparator
                .comparingInt(RealtimeListenerInvoker::order)
                .thenComparing(RealtimeListenerInvoker::description)));
        return listeners;
    }

    private List<RealtimeListenerInvoker> methodInvokers(Object bean, Class<?> targetClass) {
        Map<Method, RealtimeListener> methods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<RealtimeListener>) method ->
                        AnnotationUtils.findAnnotation(method, RealtimeListener.class));
        List<RealtimeListenerInvoker> invokers = new ArrayList<>();
        for (Map.Entry<Method, RealtimeListener> entry : methods.entrySet()) {
            Method method = AopUtils.selectInvocableMethod(entry.getKey(), bean.getClass());
            validateListenerMethod(method);
            ReflectionUtils.makeAccessible(method);
            invokers.add(new MethodRealtimeListenerInvoker(bean, method, entry.getValue()));
        }
        return invokers;
    }

    private void validateListenerMethod(Method method) {
        if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != RealtimeInboundMessage.class) {
            throw new IllegalStateException("@RealtimeListener method must have exactly one RealtimeInboundMessage "
                    + "parameter: " + method);
        }
    }

    private void addInvokers(Map<String, List<RealtimeListenerInvoker>> listeners,
                             List<RealtimeListenerInvoker> invokers) {
        invokers.forEach(invoker -> addInvoker(listeners, invoker));
    }

    private void addInvoker(Map<String, List<RealtimeListenerInvoker>> listeners, RealtimeListenerInvoker invoker) {
        for (String type : invoker.types()) {
            if (type != null && !type.isBlank()) {
                listeners.computeIfAbsent(type, key -> new ArrayList<>()).add(invoker);
            }
        }
    }

    private interface RealtimeListenerInvoker {

        String[] types();

        int order();

        String description();

        void invoke(RealtimeInboundMessage message);
    }

    private record MethodRealtimeListenerInvoker(
            Object bean,
            Method method,
            RealtimeListener listener) implements RealtimeListenerInvoker {

        @Override
        public String[] types() {
            return listener.types();
        }

        @Override
        public int order() {
            return listener.order();
        }

        @Override
        public String description() {
            return bean.getClass().getName() + "#" + method.getName();
        }

        @Override
        public void invoke(RealtimeInboundMessage message) {
            ReflectionUtils.invokeMethod(method, bean, message);
        }
    }

    private record SubscriberRealtimeListenerInvoker(
            RealtimeSubscriber subscriber,
            RealtimeListener listener,
            Class<?> targetClass) implements RealtimeListenerInvoker {

        @Override
        public String[] types() {
            return listener.types();
        }

        @Override
        public int order() {
            return listener.order();
        }

        @Override
        public String description() {
            return targetClass.getName();
        }

        @Override
        public void invoke(RealtimeInboundMessage message) {
            subscriber.onMessage(message);
        }
    }
}
