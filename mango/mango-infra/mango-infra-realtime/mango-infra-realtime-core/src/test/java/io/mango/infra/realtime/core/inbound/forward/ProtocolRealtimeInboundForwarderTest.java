package io.mango.infra.realtime.core.inbound.forward;

import io.mango.infra.realtime.api.dto.RealtimeContext;
import io.mango.infra.realtime.api.dto.RealtimeEvent;
import io.mango.infra.realtime.api.dto.RealtimeInboundMessage;
import io.mango.infra.realtime.api.dto.RealtimeOutboundMessage;
import io.mango.infra.realtime.api.dto.RealtimePayload;
import io.mango.infra.realtime.api.dto.RealtimeSource;
import io.mango.infra.realtime.api.dto.RealtimeTarget;
import io.mango.infra.realtime.core.outbound.IRealtimePublishService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolRealtimeInboundForwarderTest {

    @Test
    void publishesOnlyAuthorizedClientTargetsAfterBusinessDispatch() {
        List<String> actions = new ArrayList<>();
        RecordingPublishService publisher = new RecordingPublishService(actions);
        ProtocolRealtimeInboundForwarder allowed = new ProtocolRealtimeInboundForwarder(
                message -> actions.add("dispatch"), () -> publisher, message -> true);
        ProtocolRealtimeInboundForwarder denied = new ProtocolRealtimeInboundForwarder(
                message -> actions.add("dispatch-denied"), () -> publisher, message -> false);

        allowed.forward(message("allowed"));
        denied.forward(message("denied"));

        assertThat(actions).containsExactly("dispatch", "publish:allowed", "dispatch-denied");
        assertThat(publisher.messages).extracting(RealtimeOutboundMessage::content).containsExactly("allowed");
    }

    private RealtimeInboundMessage message(String content) {
        return new RealtimeInboundMessage(
                null, null, RealtimeEvent.of("chat", "send"),
                new RealtimeSource("web", "client-a", "session-a"),
                RealtimeContext.of("tenant-a", 1001L), RealtimeTarget.group("room-a"), null,
                RealtimePayload.text(content), null, null, null, null);
    }

    private static final class RecordingPublishService implements IRealtimePublishService {

        private final List<String> actions;
        private final List<RealtimeOutboundMessage> messages = new ArrayList<>();

        private RecordingPublishService(List<String> actions) {
            this.actions = actions;
        }

        @Override
        public void publish(RealtimeOutboundMessage message) {
            messages.add(message);
            actions.add("publish:" + message.content());
        }

        @Override
        public void publishLocal(RealtimeOutboundMessage message) {
            publish(message);
        }
    }
}
