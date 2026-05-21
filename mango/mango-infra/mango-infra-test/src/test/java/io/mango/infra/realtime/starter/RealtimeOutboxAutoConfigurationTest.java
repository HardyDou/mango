package io.mango.infra.realtime.starter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxStatus;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.core.outbound.RealtimeProtocolSender;
import io.mango.infra.realtime.starter.outbox.RealtimeOutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeOutboxAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    io.mango.infra.kv.starter.OutboxAutoConfiguration.class,
                    MangoRealtimeAutoConfiguration.class))
            .withPropertyValues(
                    "mango.infra.realtime.mode=polling",
                    "mango.infra.realtime.polling.enabled=true",
                    "mango.kv.capability.enabled=true",
                    "mango.kv.capability.outbox=true",
                    "mango.infra.realtime.outbox.initial-delay-millis=60000")
            .withBean(IKvStore.class, MemoryKvStore::new);

    @Test
    void realtimeApi_enqueuesMessageBeforeDispatcherPublishes() {
        CapturingSender sender = new CapturingSender();
        contextRunner
                .withBean(RealtimeProtocolSender.class, () -> sender)
                .run(context -> {
                    RealtimeApi realtimeApi = context.getBean(RealtimeApi.class);
                    IOutboxStore outboxStore = context.getBean(IOutboxStore.class);

                    realtimeApi.broadcast("chat.message", "hello-outbox");

                    assertThat(sender.messages).isEmpty();
                    assertThat(outboxStore.claim("assert-worker", 10, Instant.now()))
                            .hasSize(1)
                            .first()
                            .satisfies(message -> {
                                assertThat(message.getEventType()).isEqualTo("realtime.message.dispatch");
                                assertThat(message.getPayload()).containsKey("message");
                            });
                });
    }

    @Test
    void dispatcher_claimsOutboxMessageAndPublishesLocally() {
        CapturingSender sender = new CapturingSender();
        contextRunner
                .withBean(RealtimeProtocolSender.class, () -> sender)
                .run(context -> {
                    RealtimeApi realtimeApi = context.getBean(RealtimeApi.class);
                    RealtimeOutboxDispatcher dispatcher = context.getBean(RealtimeOutboxDispatcher.class);

                    realtimeApi.broadcast("chat.message", "hello-dispatcher");
                    dispatcher.dispatchReadyMessages();

                    assertThat(sender.messages)
                            .extracting(RealtimeOutboundMessage::content)
                            .contains("hello-dispatcher");
                });
    }

    @Test
    void dispatcher_claimsOnlyRealtimeOutboxMessages() {
        CapturingSender sender = new CapturingSender();
        contextRunner
                .withBean(RealtimeProtocolSender.class, () -> sender)
                .run(context -> {
                    IOutboxStore outboxStore = context.getBean(IOutboxStore.class);
                    RealtimeApi realtimeApi = context.getBean(RealtimeApi.class);
                    RealtimeOutboxDispatcher dispatcher = context.getBean(RealtimeOutboxDispatcher.class);
                    OutboxMessage workflowMessage = OutboxMessage.builder()
                            .eventType("workflow.process.completed")
                            .businessType("workflow")
                            .businessKey("EXP-1")
                            .aggregateId("PROC-1")
                            .occurredAt(Instant.now())
                            .build();

                    outboxStore.enqueue(workflowMessage);
                    realtimeApi.broadcast("chat.message", "hello-realtime-only");

                    dispatcher.dispatchReadyMessages();

                    assertThat(sender.messages)
                            .extracting(RealtimeOutboundMessage::content)
                            .containsExactly("hello-realtime-only");
                    assertThat(outboxStore.claim("assert-worker", 10, Instant.now()))
                            .hasSize(1)
                            .first()
                            .satisfies(message -> {
                                assertThat(message.getMessageId()).isEqualTo(workflowMessage.getMessageId());
                                assertThat(message.getEventType()).isEqualTo("workflow.process.completed");
                                assertThat(message.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
                            });
                });
    }

    private static final class CapturingSender implements RealtimeProtocolSender {
        private final List<RealtimeOutboundMessage> messages = new ArrayList<>();

        @Override
        public String protocol() {
            return "test";
        }

        @Override
        public void sendToUser(Long userId, RealtimeOutboundMessage envelope) {
            messages.add(envelope);
        }

        @Override
        public void sendToClient(String tenantId, String clientId, RealtimeOutboundMessage envelope) {
            messages.add(envelope);
        }

        @Override
        public void sendToConnection(String connectionId, RealtimeOutboundMessage envelope) {
            messages.add(envelope);
        }

        @Override
        public void sendToGroup(String tenantId, String groupId, RealtimeOutboundMessage envelope) {
            messages.add(envelope);
        }

        @Override
        public void sendToTenant(String tenantId, RealtimeOutboundMessage envelope) {
            messages.add(envelope);
        }

        @Override
        public void broadcast(RealtimeOutboundMessage envelope) {
            messages.add(envelope);
        }
    }
}
