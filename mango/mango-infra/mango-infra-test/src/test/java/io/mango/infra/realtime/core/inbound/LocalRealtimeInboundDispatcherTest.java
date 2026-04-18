package io.mango.infra.realtime.core.inbound;

import io.mango.infra.realtime.api.RealtimeInboundMessage;
import io.mango.infra.realtime.api.RealtimeListener;
import io.mango.infra.realtime.api.RealtimeSubscriber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalRealtimeInboundDispatcherTest {

    @Test
    void dispatch_methodLevelListener_invokesMatchingType() {
        MethodListener listener = new MethodListener();
        LocalRealtimeInboundDispatcher dispatcher = dispatcherWith("listener", listener);

        dispatcher.dispatch(message("task.cancel"));

        assertEquals(List.of("method:task.cancel"), listener.events);
    }

    @Test
    void dispatch_classLevelSubscriber_invokesSubscriber() {
        ClassLevelSubscriber listener = new ClassLevelSubscriber();
        LocalRealtimeInboundDispatcher dispatcher = dispatcherWith("listener", listener);

        dispatcher.dispatch(message("task.pause"));

        assertEquals(List.of("class:task.pause"), listener.events);
    }

    @Test
    void dispatch_methodLevelOverridesClassLevelForSameBean() {
        MixedListener listener = new MixedListener();
        LocalRealtimeInboundDispatcher dispatcher = dispatcherWith("listener", listener);

        dispatcher.dispatch(message("task.cancel"));

        assertEquals(List.of("method:task.cancel"), listener.events);
    }

    @Test
    void dispatch_unknownTypeError_throws() {
        LocalRealtimeInboundDispatcher dispatcher = dispatcherWith(
                InboundUnknownTypePolicy.ERROR,
                "listener",
                new MethodListener());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(message("unknown")));
    }

    @Test
    void hasListeners_falseWhenNoListenerBeans() {
        LocalRealtimeInboundDispatcher dispatcher = dispatcherWith("bean", new Object());

        assertTrue(!dispatcher.hasListeners());
    }

    private LocalRealtimeInboundDispatcher dispatcherWith(String beanName, Object bean) {
        return dispatcherWith(InboundUnknownTypePolicy.IGNORE, beanName, bean);
    }

    private LocalRealtimeInboundDispatcher dispatcherWith(InboundUnknownTypePolicy policy, String beanName, Object bean) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean(beanName, bean);
        return new LocalRealtimeInboundDispatcher(beanFactory, false, policy);
    }

    private RealtimeInboundMessage message(String type) {
        return new RealtimeInboundMessage(null, type, "{}", "tenant-a", 1L, "s1", null, null);
    }

    static class MethodListener {

        private final List<String> events = new ArrayList<>();

        @RealtimeListener(types = "task.cancel")
        public void onCancel(RealtimeInboundMessage message) {
            events.add("method:" + message.type());
        }
    }

    @RealtimeListener(types = "task.pause")
    static class ClassLevelSubscriber implements RealtimeSubscriber {

        private final List<String> events = new ArrayList<>();

        @Override
        public void onMessage(RealtimeInboundMessage message) {
            events.add("class:" + message.type());
        }
    }

    @RealtimeListener(types = "task.cancel")
    static class MixedListener implements RealtimeSubscriber {

        private final List<String> events = new ArrayList<>();

        @RealtimeListener(types = "task.cancel")
        public void onCancel(RealtimeInboundMessage message) {
            events.add("method:" + message.type());
        }

        @Override
        public void onMessage(RealtimeInboundMessage message) {
            events.add("class:" + message.type());
        }
    }
}
