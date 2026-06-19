package io.mango.infra.web.filter;

import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * 在 HTTP 请求处理期间将 Mango 请求上下文写入 MDC。
 */
public class WebMdcFilter implements Filter {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String CLIENT_IP_KEY = "clientIp";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            MangoContextSnapshot context = MangoContextHolder.get();
            putIfPresent(REQUEST_ID_KEY, context.requestId());
            putIfPresent(TRACE_ID_KEY, context.traceId());
            putIfPresent(CLIENT_IP_KEY, context.clientIp());
            chain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_KEY);
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(CLIENT_IP_KEY);
        }
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            MDC.put(key, value);
        }
    }
}
