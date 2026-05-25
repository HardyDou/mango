package io.mango.infra.web.filter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.web.api.IInternalPathProvider;
import io.mango.infra.web.starter.MangoWebProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.nio.charset.StandardCharsets;
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
    @DisplayName("internal path with internal header should be rejected without secret")
    void internalPathWithHeaderShouldBeRejectedWithoutSecret() throws Exception {
        InternalCallFilter filter = newFilter(List.of("/internal/**"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/config");
        request.addHeader("X-Internal-Call", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(invoked));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("configured mango web inner secret should require timestamp nonce and signature")
    void configuredSecretShouldRequireSignedHeaders() throws Exception {
        MangoWebProperties properties = new MangoWebProperties();
        properties.getInner().setSecret("test-secret");
        InternalCallFilter filter = newFilter(List.of("/private/**"), properties, new InMemoryKvStore());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private/config");
        request.addHeader("X-Internal-Call", "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(invoked));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("valid signed internal call should pass once and reject replayed nonce")
    void validSignedInternalCallShouldPassOnceAndRejectReplay() throws Exception {
        InMemoryKvStore kvStore = new InMemoryKvStore();
        MangoWebProperties properties = new MangoWebProperties();
        properties.getInner().setSecret("test-secret");
        InternalCallFilter filter = newFilter(List.of("/private/**"), properties, kvStore);
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-1";
        String signature = hmacSha256(timestamp + ":" + nonce + ":POST:/private/config:a=1&b=2", "test-secret");
        AtomicBoolean firstInvoked = new AtomicBoolean(false);

        filter.doFilter(signedRequest(timestamp, nonce, signature), new MockHttpServletResponse(), chain(firstInvoked));

        assertTrue(firstInvoked.get());
        MockHttpServletResponse replayResponse = new MockHttpServletResponse();
        AtomicBoolean replayInvoked = new AtomicBoolean(false);
        filter.doFilter(signedRequest(timestamp, nonce, signature), replayResponse, chain(replayInvoked));

        assertFalse(replayInvoked.get());
        assertEquals(403, replayResponse.getStatus());
    }

    private InternalCallFilter newFilter(List<String> internalPaths) {
        return newFilter(internalPaths, new MangoWebProperties(), new InMemoryKvStore());
    }

    private InternalCallFilter newFilter(List<String> internalPaths, MangoWebProperties properties, IKvStore kvStore) {
        IInternalPathProvider provider = () -> internalPaths;
        InternalCallFilter filter = new InternalCallFilter(provider, kvStore, properties);
        filter.onApplicationReady();
        return filter;
    }

    private FilterChain chain(AtomicBoolean invoked) {
        return (request, response) -> invoked.set(true);
    }

    private MockHttpServletRequest signedRequest(long timestamp, String nonce, String signature) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/private/config");
        request.setQueryString("b=2&a=1");
        request.addHeader("X-Internal-Call", "true");
        request.addHeader("X-Internal-Timestamp", Long.toString(timestamp));
        request.addHeader("X-Internal-Nonce", nonce);
        request.addHeader("X-Internal-Signature", signature);
        return request;
    }

    private String hmacSha256(String data, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hmacBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
