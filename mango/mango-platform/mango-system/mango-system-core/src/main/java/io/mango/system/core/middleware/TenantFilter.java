package io.mango.system.core.middleware;

import io.mango.infra.context.api.MangoContextHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Tenant isolation filter - extracts tenant from request header or JWT.
 */
@Slf4j
@Component
public class TenantFilter extends OncePerRequestFilter {

    public static final String TENANT_ID_HEADER = MangoContextHeaders.TENANT_ID;
    public static final String LEGACY_TENANT_ID_HEADER = "TENANT-ID";
    public static final String COMPAT_TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String TENANT_ID_KEY = "tenantId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantIdHeader = firstText(
                request.getHeader(TENANT_ID_HEADER),
                request.getHeader(LEGACY_TENANT_ID_HEADER),
                request.getHeader(COMPAT_TENANT_ID_HEADER));
        if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
            try {
                request.setAttribute(TENANT_ID_KEY, Long.parseLong(tenantIdHeader));
            } catch (NumberFormatException e) {
                log.warn("Invalid tenant ID header: {}", tenantIdHeader);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
