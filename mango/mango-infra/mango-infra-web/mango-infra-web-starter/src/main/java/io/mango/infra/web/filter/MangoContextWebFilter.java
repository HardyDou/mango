package io.mango.infra.web.filter;

import io.mango.infra.context.core.MangoContextHeaders;
import io.mango.infra.context.core.MangoContextHolder;
import io.mango.infra.context.core.MangoContextSnapshot;
import io.mango.infra.web.api.IRequestContextProvider;
import io.mango.infra.web.api.RequestContextSnapshot;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * 将 HTTP 请求上下文初始化为 Mango 运行时上下文。
 *
 * @author Mango
 */
public class MangoContextWebFilter implements Filter {

    private final IRequestContextProvider requestContextProvider;

    public MangoContextWebFilter(IRequestContextProvider requestContextProvider) {
        this.requestContextProvider = Objects.requireNonNull(requestContextProvider, "requestContextProvider");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            RequestContextSnapshot requestContext = requestContextProvider.currentContext();
            MangoContextHolder.set(MangoContextSnapshot.request(
                    requestContext.requestId(),
                    requestContext.traceId(),
                    header(requestContext.headers(), MangoContextHeaders.TENANT_ID),
                    header(requestContext.headers(), MangoContextHeaders.APP_CODE),
                    requestContext.clientIp()));
            chain.doFilter(request, response);
        } finally {
            MangoContextHolder.clear();
        }
    }

    private String header(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
