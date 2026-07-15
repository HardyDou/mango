package io.mango.infra.event.core;

import io.mango.infra.event.api.DomainEvent;
import io.mango.infra.event.core.memory.InMemoryDomainEventBus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEventContractTest {

    @Test
    void builtEvent_shouldIsolatePayloadAndHeadersFromPublisherAndSubscriberMutation() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", 100);
        payload.put("optional", null);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("tenantId", "1");

        DomainEvent event = DomainEvent.builder()
                .eventType("payment.succeeded")
                .payload(payload)
                .headers(headers)
                .build();
        payload.put("amount", 200);
        headers.put("tenantId", "2");

        assertThat(event.getPayload()).containsEntry("amount", 100).containsEntry("optional", null);
        assertThat(event.getHeaders()).containsEntry("tenantId", "1");
        assertThatThrownBy(() -> event.getPayload().put("amount", 300))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> event.getHeaders().put("tenantId", "3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void publish_whenOneHandlerFails_shouldContinueOtherHandlersAndAggregateFailure() {
        InMemoryDomainEventBus eventBus = new InMemoryDomainEventBus();
        List<String> handled = new ArrayList<>();
        IllegalStateException firstFailure = new IllegalStateException("first rejected");
        IllegalArgumentException secondFailure = new IllegalArgumentException("second rejected");
        eventBus.subscribe("workflow.completed", ignored -> {
            handled.add("first");
            throw firstFailure;
        });
        eventBus.subscribe("workflow.completed", ignored -> handled.add("middle"));
        eventBus.subscribe("workflow.completed", ignored -> {
            handled.add("last");
            throw secondFailure;
        });

        DomainEvent event = DomainEvent.builder().eventType("workflow.completed").build();

        assertThatThrownBy(() -> eventBus.publish(event))
                .isSameAs(firstFailure)
                .satisfies(error -> assertThat(error.getSuppressed()).containsExactly(secondFailure));
        assertThat(handled).containsExactly("first", "middle", "last");
    }

    @Test
    void subscriptionClose_shouldStopOnlyClosedHandler() throws Exception {
        InMemoryDomainEventBus eventBus = new InMemoryDomainEventBus();
        List<String> handled = new ArrayList<>();
        AutoCloseable exact = eventBus.subscribe("order.created", ignored -> handled.add("exact"));
        eventBus.subscribe(InMemoryDomainEventBus.WILDCARD, ignored -> handled.add("wildcard"));

        DomainEvent event = DomainEvent.builder().eventType("order.created").build();
        eventBus.publish(event);
        exact.close();
        exact.close();
        eventBus.publish(event);

        assertThat(handled).containsExactly("exact", "wildcard", "wildcard");
    }
}
