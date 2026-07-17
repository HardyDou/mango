package io.mango.infra.web.filter;

import io.mango.infra.kv.api.IKvStore;
import io.mango.infra.web.api.IInternalPathProvider;
import io.mango.infra.web.support.InternalCallAttributes;
import io.mango.infra.web.starter.MangoWebProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    void doFilter_pathDiscoveryFailure_rejectsEveryRequest() throws Exception {
        IInternalPathProvider failingProvider = () -> {
            throw new IllegalStateException("path registry unavailable");
        };
        InternalCallFilter filter = new InternalCallFilter(
                failingProvider, new InMemoryKvStore(), new MangoWebProperties());
        filter.onApplicationStarted();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/public/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, chain(invoked));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Internal paths not loaded"));
    }

    @Test
    void doFilter_springPathPatterns_respectBoundariesAndVariables() throws Exception {
        InternalCallFilter wildcardFilter = newFilter(List.of("/private/**"));
        AtomicBoolean similarPrefixInvoked = new AtomicBoolean(false);
        wildcardFilter.doFilter(new MockHttpServletRequest("GET", "/privateer/config"),
                new MockHttpServletResponse(), chain(similarPrefixInvoked));

        InternalCallFilter variableFilter = newFilter(List.of("/orders/{id}"));
        AtomicBoolean variablePathInvoked = new AtomicBoolean(false);
        MockHttpServletResponse variableResponse = new MockHttpServletResponse();
        variableFilter.doFilter(new MockHttpServletRequest("GET", "/orders/42"),
                variableResponse, chain(variablePathInvoked));

        assertTrue(similarPrefixInvoked.get());
        assertFalse(variablePathInvoked.get());
        assertEquals(403, variableResponse.getStatus());
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

    @Test
    void validSignedInternalCall_marksRequestAsServerVerified() throws Exception {
        MangoWebProperties properties = new MangoWebProperties();
        properties.getInner().setSecret("test-secret");
        InternalCallFilter filter = newFilter(List.of("/private/**"), properties, new InMemoryKvStore());
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-verified-attribute";
        String signature = hmacSha256(
                timestamp + ":" + nonce + ":POST:/private/config:a=1&b=2", "test-secret");
        MockHttpServletRequest request = signedRequest(timestamp, nonce, signature);
        AtomicBoolean verifiedInChain = new AtomicBoolean(false);

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                verifiedInChain.set(Boolean.TRUE.equals(
                        servletRequest.getAttribute(InternalCallAttributes.VERIFIED))));

        assertTrue(verifiedInChain.get());
        assertEquals(Ordered.HIGHEST_PRECEDENCE + 20, filter.getOrder());
    }

    @Test
    void doFilter_atomicNonceClaimFailure_rejectsRequest() throws Exception {
        MangoWebProperties properties = new MangoWebProperties();
        properties.getInner().setSecret("test-secret");
        IKvStore rejectingStore = new InMemoryKvStore() {
            @Override
            public boolean put(String key, String value, long expireSeconds) {
                return false;
            }
        };
        InternalCallFilter filter = newFilter(List.of("/private/**"), properties, rejectingStore);
        long timestamp = System.currentTimeMillis();
        String signature = hmacSha256(timestamp + ":nonce-rejected:POST:/private/config:a=1&b=2", "test-secret");
        AtomicBoolean invoked = new AtomicBoolean(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(signedRequest(timestamp, "nonce-rejected", signature), response, chain(invoked));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
    }

    @Test
    void doFilter_concurrentReplay_allowsExactlyOneRequest() throws Exception {
        CoordinatedKvStore kvStore = new CoordinatedKvStore();
        MangoWebProperties properties = new MangoWebProperties();
        properties.getInner().setSecret("test-secret");
        InternalCallFilter filter = newFilter(List.of("/private/**"), properties, kvStore);
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce-concurrent";
        String signature = hmacSha256(timestamp + ":" + nonce + ":POST:/private/config:a=1&b=2", "test-secret");
        AtomicInteger invoked = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> invoke(filter, timestamp, nonce, signature, invoked));
            Future<?> second = executor.submit(() -> invoke(filter, timestamp, nonce, signature, invoked));
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, invoked.get());
    }

    private InternalCallFilter newFilter(List<String> internalPaths) {
        return newFilter(internalPaths, new MangoWebProperties(), new InMemoryKvStore());
    }

    private InternalCallFilter newFilter(List<String> internalPaths, MangoWebProperties properties, IKvStore kvStore) {
        IInternalPathProvider provider = () -> internalPaths;
        InternalCallFilter filter = new InternalCallFilter(provider, kvStore, properties);
        filter.onApplicationStarted();
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

    private void invoke(InternalCallFilter filter, long timestamp, String nonce, String signature,
                        AtomicInteger invoked) {
        try {
            filter.doFilter(signedRequest(timestamp, nonce, signature), new MockHttpServletResponse(),
                    (request, response) -> invoked.incrementAndGet());
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static class InMemoryKvStore implements IKvStore {

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

    private static final class CoordinatedKvStore extends InMemoryKvStore {

        private final CyclicBarrier existsBarrier = new CyclicBarrier(2);

        @Override
        public boolean exists(String key) {
            try {
                existsBarrier.await();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
            return super.exists(key);
        }
    }
}
