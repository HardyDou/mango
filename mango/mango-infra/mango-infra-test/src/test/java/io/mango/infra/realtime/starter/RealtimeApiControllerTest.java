package io.mango.infra.realtime.starter;

import io.mango.infra.realtime.api.RealtimeMessage;
import io.mango.infra.realtime.api.RealtimePublisher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeApiControllerTest {

    @Test
    void publish_delegatesToRealtimePublisher() {
        RecordingPublisher publisher = new RecordingPublisher();
        RealtimeApiController controller = new RealtimeApiController(publisher);
        RealtimeMessage message = RealtimeMessage.toUser(1001L, "notice", "remote-message");

        controller.publish(message);

        assertEquals(message, publisher.lastMessage);
    }

    private static class RecordingPublisher implements RealtimePublisher {

        private RealtimeMessage lastMessage;

        @Override
        public void publish(RealtimeMessage envelope) {
            lastMessage = envelope;
        }
    }
}
