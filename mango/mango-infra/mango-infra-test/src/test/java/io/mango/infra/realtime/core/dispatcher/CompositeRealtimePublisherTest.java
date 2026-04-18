package io.mango.infra.realtime.core.dispatcher;

import io.mango.infra.realtime.api.RealtimeMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeRealtimePublisherTest {

    @Test
    void publish_userEnvelope_routesToUserOnAllSenders() {
        RecordingSender sender = new RecordingSender();
        CompositeRealtimePublisher publisher = new CompositeRealtimePublisher(List.of(sender));

        publisher.publish(RealtimeMessage.toUser(9L, "message", "payload"));

        assertEquals("user:9", sender.lastRoute);
    }

    @Test
    void publish_tenantEnvelope_routesToTenant() {
        RecordingSender sender = new RecordingSender();
        CompositeRealtimePublisher publisher = new CompositeRealtimePublisher(List.of(sender));

        publisher.publish(RealtimeMessage.toTenant("tenant-a", "message", "payload"));

        assertEquals("tenant:tenant-a", sender.lastRoute);
    }

    @Test
    void publish_senderThrows_continuesWithNextSender() {
        RecordingSender sender = new RecordingSender();
        CompositeRealtimePublisher publisher = new CompositeRealtimePublisher(List.of(new FailingSender(), sender));

        publisher.publish(RealtimeMessage.of("message", "payload"));

        assertEquals("broadcast", sender.lastRoute);
    }

    private static class RecordingSender implements ProtocolRealtimeSender {
        private String lastRoute;

        @Override
        public String protocol() {
            return "TEST";
        }

        @Override
        public void sendToUser(Long userId, RealtimeMessage envelope) {
            lastRoute = "user:" + userId;
        }

        @Override
        public void sendToTenant(String tenantId, RealtimeMessage envelope) {
            lastRoute = "tenant:" + tenantId;
        }

        @Override
        public void broadcast(RealtimeMessage envelope) {
            lastRoute = "broadcast";
        }
    }

    private static class FailingSender extends RecordingSender {
        @Override
        public void broadcast(RealtimeMessage envelope) {
            throw new IllegalStateException("boom");
        }
    }
}
