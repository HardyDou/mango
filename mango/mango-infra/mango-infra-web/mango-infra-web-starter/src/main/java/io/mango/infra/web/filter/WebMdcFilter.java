package io.mango.infra.web.filter;

import io.mango.infra.web.api.IRequestContextProvider;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Adds Mango request context values to MDC during HTTP request handling.
 */
public class WebMdcFilter implements Filter {

    public static final String REQUEST_ID_KEY = "requestId";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String CLIENT_IP_KEY = "clientIp";

    private final IRequestContextProvider requestContextProvider;

    public WebMdcFilter(IRequestContextProvider requestContextProvider) {
        this.requestContextProvider = requestContextProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            var context = requestContextProvider.currentContext();
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
