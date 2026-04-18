package io.mango.infra.realtime.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealtimeMessageTest {

    @Test
    void constructor_blankDefaults_normalizesEnvelope() {
        RealtimeMessage envelope = new RealtimeMessage("", "", "payload", "", null, null, null);

        assertNotNull(envelope.id());
        assertEquals("message", envelope.type());
        assertEquals("default", envelope.tenantId());
        assertTrue(envelope.headers().isEmpty());
        assertNotNull(envelope.createdAt());
    }

    @Test
    void constructor_mutableHeaders_copiesHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("source", "unit-test");

        RealtimeMessage envelope = new RealtimeMessage(null, "notice", "payload", "t1", 1L, headers, null);
        headers.put("source", "changed");

        assertEquals("unit-test", envelope.headers().get("source"));
        assertThrows(UnsupportedOperationException.class, () -> envelope.headers().put("x", "y"));
    }

    @Test
    void factories_validInputs_setRoutingFields() {
        RealtimeMessage userEnvelope = RealtimeMessage.toUser(7L, "chat", "hello");
        RealtimeMessage tenantEnvelope = RealtimeMessage.toTenant("tenant-a", "alert", "warn");

        assertEquals(7L, userEnvelope.userId());
        assertEquals("chat", userEnvelope.type());
        assertEquals("tenant-a", tenantEnvelope.tenantId());
        assertEquals("alert", tenantEnvelope.type());
    }
}
