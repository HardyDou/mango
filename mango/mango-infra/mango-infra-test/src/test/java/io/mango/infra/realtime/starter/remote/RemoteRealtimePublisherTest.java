package io.mango.infra.realtime.starter.remote;

import io.mango.infra.realtime.api.RealtimeApi;
import io.mango.infra.realtime.api.RealtimeMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteRealtimePublisherTest {

    @Test
    void publish_delegatesToRemoteApi() {
        RecordingRealtimeApi realtimeApi = new RecordingRealtimeApi();
        RemoteRealtimePublisher publisher = new RemoteRealtimePublisher(realtimeApi);
        RealtimeMessage message = RealtimeMessage.toTenant("tenant-a", "notice", "remote-message");

        publisher.publish(message);

        assertEquals(message, realtimeApi.lastMessage);
    }

    private static class RecordingRealtimeApi implements RealtimeApi {

        private RealtimeMessage lastMessage;

        @Override
        public void publish(RealtimeMessage message) {
            lastMessage = message;
        }
    }
}
