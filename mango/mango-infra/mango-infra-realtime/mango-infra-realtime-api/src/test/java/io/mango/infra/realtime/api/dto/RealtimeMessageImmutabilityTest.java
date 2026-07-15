package io.mango.infra.realtime.api.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeMessageImmutabilityTest {

    @Test
    void inboundMessageOwnsPayloadSnapshot() {
        RealtimePayload original = RealtimePayload.text("before");
        RealtimeInboundMessage message = new RealtimeInboundMessage(
                null, null, null, null, null, null, null, original, null, null, null, null);

        original.put("text", "mutated-input");
        message.payload().put("text", "mutated-accessor");

        assertThat(message.content()).isEqualTo("before");
    }

    @Test
    void outboundMessageOwnsPayloadSnapshot() {
        RealtimePayload original = RealtimePayload.text("before");
        RealtimeOutboundMessage message = new RealtimeOutboundMessage(
                null, null, null, null, null, null, null, original, null, null, null, null, null);

        original.put("text", "mutated-input");
        message.payload().put("text", "mutated-accessor");

        assertThat(message.content()).isEqualTo("before");
    }
}
