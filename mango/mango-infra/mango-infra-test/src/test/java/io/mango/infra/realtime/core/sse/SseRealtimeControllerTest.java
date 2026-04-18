package io.mango.infra.realtime.core.sse;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SseRealtimeControllerTest {

    @Test
    void connect_validAuthorization_delegatesToProtocolAdapter() {
        SseProtocolAdapter adapter = mock(SseProtocolAdapter.class);
        SseEmitter emitter = new SseEmitter(30_000L);
        when(adapter.createEmitter("tenant-a", 1001L)).thenReturn(emitter);
        SseRealtimeController controller = new SseRealtimeController(adapter);

        SseEmitter result = controller.connect("Bearer token", "tenant-a", 1001L);

        assertSame(emitter, result);
        verify(adapter).createEmitter("tenant-a", 1001L);
    }

    @Test
    void connect_missingAuthorization_returnsErrorEmitterWithoutSubscribing() {
        SseProtocolAdapter adapter = mock(SseProtocolAdapter.class);
        SseRealtimeController controller = new SseRealtimeController(adapter);

        SseEmitter result = controller.connect(null, "tenant-a", 1001L);

        assertNotNull(result);
        verify(adapter, never()).createEmitter("tenant-a", 1001L);
    }
}
