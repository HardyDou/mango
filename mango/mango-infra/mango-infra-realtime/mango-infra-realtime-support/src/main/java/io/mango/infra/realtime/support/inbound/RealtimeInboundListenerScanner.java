package io.mango.infra.realtime.support.inbound;

import io.mango.infra.realtime.api.annotation.RealtimeInboundMessageListener;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
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

public class RealtimeInboundListenerScanner {

    public Map<String, List<RealtimeInboundListenerInvoker>> scan(ListableBeanFactory beanFactory) {
        Map<String, List<RealtimeInboundListenerInvoker>> listeners = new LinkedHashMap<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Object bean = beanFactory.getBean(beanName);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            List<RealtimeInboundListenerInvoker> methodInvokers = methodInvokers(bean, targetClass);
            if (!methodInvokers.isEmpty()) {
                addInvokers(listeners, methodInvokers);
            }
        }
        listeners.values().forEach(invokers -> invokers.sort(Comparator
                .comparingInt(RealtimeInboundListenerInvoker::order)
                .thenComparing(RealtimeInboundListenerInvoker::description)));
        return listeners;
    }

    private List<RealtimeInboundListenerInvoker> methodInvokers(Object bean, Class<?> targetClass) {
        Map<Method, RealtimeInboundMessageListener> methods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<RealtimeInboundMessageListener>) method ->
                        AnnotationUtils.findAnnotation(method, RealtimeInboundMessageListener.class));
        List<RealtimeInboundListenerInvoker> invokers = new ArrayList<>();
        for (Map.Entry<Method, RealtimeInboundMessageListener> entry : methods.entrySet()) {
            Method method = AopUtils.selectInvocableMethod(entry.getKey(), bean.getClass());
            validateListenerMethod(method);
            ReflectionUtils.makeAccessible(method);
            invokers.add(new MethodRealtimeListenerInvoker(bean, method, entry.getValue()));
        }
        return invokers;
    }

    private void validateListenerMethod(Method method) {
        if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != RealtimeInboundMessage.class) {
            throw new IllegalStateException("@RealtimeInboundMessageListener method must have exactly one "
                    + "RealtimeInboundMessage parameter: " + method);
        }
    }

    private void addInvokers(Map<String, List<RealtimeInboundListenerInvoker>> listeners,
                             List<RealtimeInboundListenerInvoker> invokers) {
        invokers.forEach(invoker -> addInvoker(listeners, invoker));
    }

    private void addInvoker(Map<String, List<RealtimeInboundListenerInvoker>> listeners,
                            RealtimeInboundListenerInvoker invoker) {
        for (String type : invoker.types()) {
            if (type != null && !type.isBlank()) {
                listeners.computeIfAbsent(type, key -> new ArrayList<>()).add(invoker);
            }
        }
    }

    private record MethodRealtimeListenerInvoker(
            Object bean,
            Method method,
            RealtimeInboundMessageListener listener) implements RealtimeInboundListenerInvoker {

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
}
