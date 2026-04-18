package io.mango.infra.realtime.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeHeadersTest {

    @Test
    void constants_areStable() {
        assertEquals("Authorization", RealtimeHeaders.AUTHORIZATION);
        assertEquals("TENANT-ID", RealtimeHeaders.TENANT_ID);
    }
}
