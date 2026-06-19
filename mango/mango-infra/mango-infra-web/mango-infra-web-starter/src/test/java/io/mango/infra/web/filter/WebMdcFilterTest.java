package io.mango.infra.web.filter;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebMdcFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        MangoContextHolder.clear();
    }

    @Test
    void filterShouldPutRequestContextIntoMdcAndClearAfterRequest() throws Exception {
        MangoContextHolder.set(MangoContextSnapshot.request("request-1", "trace-1", null, null, "127.0.0.1"));
        WebMdcFilter filter = new WebMdcFilter();
        AtomicReference<String> traceId = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> traceId.set(MDC.get(WebMdcFilter.TRACE_ID_KEY)));

        assertEquals("trace-1", traceId.get());
        assertNull(MDC.get(WebMdcFilter.TRACE_ID_KEY));
        assertNull(MDC.get(WebMdcFilter.REQUEST_ID_KEY));
        assertNull(MDC.get(WebMdcFilter.CLIENT_IP_KEY));
    }
}
