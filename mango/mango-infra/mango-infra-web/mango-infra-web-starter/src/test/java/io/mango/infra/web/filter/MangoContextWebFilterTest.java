package io.mango.infra.web.filter;

import io.mango.infra.context.core.MangoContextHeaders;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.web.api.RequestContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MangoContextWebFilterTest {

    @AfterEach
    void tearDown() {
        MangoContextHolder.clear();
    }

    @Test
    void shouldReadServletRequestHeadersWithoutRequestContextHolder() throws Exception {
        MangoContextWebFilter filter = new MangoContextWebFilter(RequestContextSnapshot::empty);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/file/files/page");
        request.addHeader(MangoContextHeaders.REQUEST_ID, "request-1");
        request.addHeader(MangoContextHeaders.TRACE_ID, "trace-1");
        request.addHeader(MangoContextHeaders.TENANT_ID, "1");
        request.addHeader(MangoContextHeaders.APP_CODE, "admin");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");
        AtomicReference<String> tenantId = new AtomicReference<>();
        AtomicReference<String> traceId = new AtomicReference<>();
        AtomicReference<String> clientIp = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {
            tenantId.set(MangoContextHolder.tenantId());
            traceId.set(MangoContextHolder.traceId());
            clientIp.set(MangoContextHolder.clientIp());
        });

        assertEquals("1", tenantId.get());
        assertEquals("trace-1", traceId.get());
        assertEquals("10.0.0.1", clientIp.get());
        assertNull(MangoContextHolder.tenantId());
    }

}
