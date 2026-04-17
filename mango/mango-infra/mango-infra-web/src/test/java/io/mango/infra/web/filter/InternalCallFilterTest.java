package io.mango.infra.web.filter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.web.api.IInternalPathProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InternalCallFilterTest {

    @Test
    @DisplayName("non-internal path should pass through filter")
    void nonInternalPathShouldPassThrough() throws Exception {
        InternalCallFilter filter = newFilter(List.of("/internal/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/profile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(invoked));

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("internal path without internal header should be rejected")
    void internalPathWithoutHeaderShouldBeRejected() throws Exception {
        InternalCallFilter filter = newFilter(List.of("/internal/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/config");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(invoked));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("internal path with internal header should pass in dev mode without secret")
    void internalPathWithHeaderShouldPassWithoutSecret() throws Exception {
        InternalCallFilter filter = newFilter(List.of("/internal/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/config");
        request.addHeader("X-Internal-Call", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(invoked));

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    private InternalCallFilter newFilter(List<String> internalPaths) {
        IInternalPathProvider provider = () -> internalPaths;
        InternalCallFilter filter = new InternalCallFilter(provider, new InMemoryKvStore());
        filter.onApplicationReady();
        return filter;
    }

    private FilterChain chain(AtomicBoolean invoked) {
        return (request, response) -> invoked.set(true);
    }

    private static final class InMemoryKvStore implements IKvStore {

        private final Map<String, String> store = new ConcurrentHashMap<>();

        @Override
        public boolean put(String key, String value, long expireSeconds) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public String get(String key) {
            return store.get(key);
        }

        @Override
        public long increment(String key, long windowSeconds) {
            throw new UnsupportedOperationException("increment not needed in this test");
        }

        @Override
        public void delete(String key) {
            store.remove(key);
        }

        @Override
        public boolean exists(String key) {
            return store.containsKey(key);
        }
    }
}
