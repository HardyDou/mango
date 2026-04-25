package io.mango.infra.web.filter;

import io.mango.infra.web.api.IRequestContextProvider;
import io.mango.infra.web.api.RequestContextSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebMdcFilterTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void filterShouldPutRequestContextIntoMdcAndClearAfterRequest() throws Exception {
        IRequestContextProvider provider = () -> new RequestContextSnapshot(
                "request-1", "trace-1", "127.0.0.1", null, Map.of(), Map.of());
        WebMdcFilter filter = new WebMdcFilter(provider);
        AtomicReference<String> traceId = new AtomicReference<>();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
                (request, response) -> traceId.set(MDC.get(WebMdcFilter.TRACE_ID_KEY)));

        assertEquals("trace-1", traceId.get());
        assertNull(MDC.get(WebMdcFilter.TRACE_ID_KEY));
        assertNull(MDC.get(WebMdcFilter.REQUEST_ID_KEY));
        assertNull(MDC.get(WebMdcFilter.CLIENT_IP_KEY));
    }
}
