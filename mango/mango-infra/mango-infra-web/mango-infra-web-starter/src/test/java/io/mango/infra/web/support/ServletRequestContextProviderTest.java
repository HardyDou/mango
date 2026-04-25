package io.mango.infra.web.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServletRequestContextProviderTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void currentContextShouldResolveStandardRequestFields() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/demo");
        request.addHeader("X-Request-Id", "request-1");
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        request.addHeader("X-Demo", "demo");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var provider = new ServletRequestContextProvider(new WebTraceIdResolver());
        var context = provider.currentContext();

        assertEquals("request-1", context.requestId());
        assertEquals("4bf92f3577b34da6a3ce929d0e0e4736", context.traceId());
        assertEquals("10.0.0.1", context.clientIp());
        assertEquals("demo", context.headers().get("X-Demo"));
    }

    @Test
    void currentContextShouldReturnEmptyOutsideWebRequest() {
        var provider = new ServletRequestContextProvider(new WebTraceIdResolver());
        var context = provider.currentContext();

        assertNull(context.requestId());
        assertNull(context.traceId());
        assertNull(context.clientIp());
    }
}
