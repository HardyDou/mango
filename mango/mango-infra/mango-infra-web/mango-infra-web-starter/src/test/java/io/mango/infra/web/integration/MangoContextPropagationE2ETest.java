package io.mango.infra.web.integration;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.infra.context.support.TtlAsync;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = MangoContextPropagationE2ETest.TestApplication.class,
        properties = {
                "spring.main.banner-mode=off",
                "mango.context.executor.core-pool-size=1",
                "mango.context.executor.max-pool-size=1",
                "mango.context.executor.thread-name-prefix=context-e2e-"
        })
class MangoContextPropagationE2ETest {

    private final TestRestTemplate restTemplate;

    @Autowired
    MangoContextPropagationE2ETest(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Test
    void httpContext_propagatesThroughRealFilterAndAsyncExecutor() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Mango-Request-Id", "request-e2e-1");
        headers.set("X-Mango-Trace-Id", "trace-e2e-1");
        headers.set("X-Mango-Tenant-Id", "tenant-e2e-1");
        headers.set("X-Mango-App-Code", "admin");
        headers.set("X-Forwarded-For", "10.10.0.1, 10.10.0.2");

        ContextView view = get(headers);

        assertEquals("request-e2e-1", view.requestId());
        assertEquals("trace-e2e-1", view.traceId());
        assertEquals("tenant-e2e-1", view.tenantId());
        assertEquals("admin", view.appCode());
        assertEquals("10.10.0.1", view.clientIp());
        assertTrue(view.threadName().startsWith("context-e2e-"));
    }

    @Test
    void consecutiveRequests_onReusedAsyncThreadDoNotLeakContext() {
        HttpHeaders firstHeaders = new HttpHeaders();
        firstHeaders.set("X-Mango-Request-Id", "request-leak-source");
        firstHeaders.set("X-Mango-Tenant-Id", "tenant-leak-source");
        firstHeaders.set("X-Mango-App-Code", "source-app");
        ContextView first = get(firstHeaders);

        ContextView second = get(new HttpHeaders());

        assertEquals(first.threadName(), second.threadName());
        assertNotEquals("request-leak-source", second.requestId());
        assertNull(second.tenantId());
        assertNull(second.appCode());
    }

    private ContextView get(HttpHeaders headers) {
        ResponseEntity<ContextView> response = restTemplate.exchange(
                "/test/context", HttpMethod.GET, new HttpEntity<>(headers), ContextView.class);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        return response.getBody();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ContextController.class, AsyncContextProbe.class})
    static class TestApplication {
    }

    @RestController
    @RequestMapping("/test/context")
    static class ContextController {

        private final AsyncContextProbe probe;

        ContextController(AsyncContextProbe probe) {
            this.probe = probe;
        }

        @GetMapping
        ContextView currentContext() throws Exception {
            return probe.capture().get(2, TimeUnit.SECONDS);
        }
    }

    @Service
    static class AsyncContextProbe {

        @TtlAsync
        CompletableFuture<ContextView> capture() {
            MangoContextSnapshot snapshot = MangoContextHolder.get();
            return CompletableFuture.completedFuture(new ContextView(
                    snapshot.requestId(),
                    snapshot.traceId(),
                    snapshot.tenantId(),
                    snapshot.appCode(),
                    snapshot.clientIp(),
                    Thread.currentThread().getName()));
        }
    }

    record ContextView(
            String requestId,
            String traceId,
            String tenantId,
            String appCode,
            String clientIp,
            String threadName
    ) {
    }
}
