package io.mango.infra.web.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestContextSnapshotTest {

    @Test
    void empty_containsNoRequestData() {
        RequestContextSnapshot snapshot = RequestContextSnapshot.empty();

        assertNull(snapshot.requestId());
        assertNull(snapshot.traceId());
        assertNull(snapshot.clientIp());
        assertNull(snapshot.request());
        assertTrue(snapshot.headers().isEmpty());
        assertTrue(snapshot.cookies().isEmpty());
    }

    @Test
    void constructor_normalizesNullMapsToImmutableEmptyMaps() {
        RequestContextSnapshot snapshot = new RequestContextSnapshot("request", "trace", "ip", new Object(), null, null);

        assertTrue(snapshot.headers().isEmpty());
        assertTrue(snapshot.cookies().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.headers().put("name", "value"));
    }

    @Test
    void constructor_defensivelyCopiesHeadersAndCookies() {
        Map<String, String> headers = new HashMap<>(Map.of("X-Test", "header"));
        Map<String, String> cookies = new HashMap<>(Map.of("session", "cookie"));
        RequestContextSnapshot snapshot = new RequestContextSnapshot("request", "trace", "ip", null, headers, cookies);

        headers.put("X-Test", "changed");
        cookies.clear();

        assertEquals(Map.of("X-Test", "header"), snapshot.headers());
        assertEquals(Map.of("session", "cookie"), snapshot.cookies());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.cookies().put("other", "value"));
    }
}
