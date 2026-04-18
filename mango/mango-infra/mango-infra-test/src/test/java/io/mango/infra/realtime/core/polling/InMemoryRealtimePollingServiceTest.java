package io.mango.infra.realtime.core.polling;

import io.mango.infra.realtime.api.RealtimeMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryRealtimePollingServiceTest {

    @Test
    void poll_existingSubscriber_returnsAndDrainsMessages() {
        InMemoryRealtimePollingService pollingService = new InMemoryRealtimePollingService();
        pollingService.append("user:1", RealtimeMessage.of("message", "one"));
        pollingService.append("user:1", RealtimeMessage.of("message", "two"));

        List<RealtimeMessage> polled = pollingService.poll("user:1", 1);

        assertEquals(1, polled.size());
        assertEquals("one", polled.get(0).content());
        assertEquals(1, pollingService.poll("user:1", 10).size());
        assertTrue(pollingService.poll("user:1", 10).isEmpty());
    }

    @Test
    void append_invalidSubscriber_ignoresMessage() {
        InMemoryRealtimePollingService pollingService = new InMemoryRealtimePollingService();

        pollingService.append("", RealtimeMessage.of("message", "ignored"));
        pollingService.append("user:1", null);

        assertTrue(pollingService.poll("", 10).isEmpty());
        assertTrue(pollingService.poll("user:1", 10).isEmpty());
    }

    @Test
    void poll_nonPositiveMax_usesDefaultLimit() {
        InMemoryRealtimePollingService pollingService = new InMemoryRealtimePollingService();
        for (int i = 0; i < 25; i++) {
            pollingService.append("user:1", RealtimeMessage.of("message", String.valueOf(i)));
        }

        assertEquals(20, pollingService.poll("user:1", 0).size());
    }

    @Test
    void poll_nonPositiveMax_usesConfiguredDefaultLimit() {
        InMemoryRealtimePollingService pollingService = new InMemoryRealtimePollingService(3);
        for (int i = 0; i < 5; i++) {
            pollingService.append("user:1", RealtimeMessage.of("message", String.valueOf(i)));
        }

        assertEquals(3, pollingService.poll("user:1", 0).size());
    }
}
