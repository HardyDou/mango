package io.mango.infra.log;

import io.mango.infra.log.layout.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * TraceIdFilter 测试
 *
 * @author Mango
 */
class TraceIdFilterTest {

    @Test
    void shouldReturnDefaultTraceIdWhenNoSourceAvailable() throws IOException, ServletException {
        MDC.remove(TraceIdFilter.TRACE_ID_KEY);

        TraceIdFilter filter = new TraceIdFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture the MDC value during filter execution
        String[] capturedTraceId = new String[1];
        FilterChain chain = (req, resp) -> capturedTraceId[0] = MDC.get(TraceIdFilter.TRACE_ID_KEY);

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId[0]).isEqualTo("-");
    }

    @Test
    void shouldExtractTraceIdFromCommonHeaders() throws IOException, ServletException {
        MDC.remove(TraceIdFilter.TRACE_ID_KEY);

        TraceIdFilter filter = new TraceIdFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedTraceId = new String[1];
        FilterChain chain = (req, resp) -> capturedTraceId[0] = MDC.get(TraceIdFilter.TRACE_ID_KEY);

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId[0]).isEqualTo("abc123");
    }

    @Test
    void shouldExtractTraceIdFromCustomHeader() throws IOException, ServletException {
        MDC.remove(TraceIdFilter.TRACE_ID_KEY);

        TraceIdFilter filter = new TraceIdFilter("X-Custom-Trace");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom-Trace", "custom-id-456");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedTraceId = new String[1];
        FilterChain chain = (req, resp) -> capturedTraceId[0] = MDC.get(TraceIdFilter.TRACE_ID_KEY);

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId[0]).isEqualTo("custom-id-456");
    }

    @Test
    void shouldPreferCustomHeaderOverCommonHeaders() throws IOException, ServletException {
        MDC.remove(TraceIdFilter.TRACE_ID_KEY);

        TraceIdFilter filter = new TraceIdFilter("X-Custom-Trace");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Custom-Trace", "custom-id");
        request.addHeader("X-Trace-Id", "common-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedTraceId = new String[1];
        FilterChain chain = (req, resp) -> capturedTraceId[0] = MDC.get(TraceIdFilter.TRACE_ID_KEY);

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId[0]).isEqualTo("custom-id");
    }

    @Test
    void shouldRemoveTraceIdAfterProcessing() throws IOException, ServletException {
        MDC.remove(TraceIdFilter.TRACE_ID_KEY);

        TraceIdFilter filter = new TraceIdFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // After filter completes, MDC should be cleared
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_KEY)).isNull();
    }

    @Test
    void shouldTryMultipleCommonHeaders() throws IOException, ServletException {
        MDC.remove(TraceIdFilter.TRACE_ID_KEY);

        TraceIdFilter filter = new TraceIdFilter(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        // X-Trace-Id 不存在，但 X-Request-Id 存在
        request.addHeader("X-Request-Id", "request-id-789");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedTraceId = new String[1];
        FilterChain chain = (req, resp) -> capturedTraceId[0] = MDC.get(TraceIdFilter.TRACE_ID_KEY);

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId[0]).isEqualTo("request-id-789");
    }
}
