package io.mango.system.core.middleware;

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

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final Long DEFAULT_TENANT_ID = 1L;
    public static final String TENANT_ID_KEY = "tenantId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantIdHeader = request.getHeader(TENANT_ID_HEADER);
        Long tenantId = DEFAULT_TENANT_ID;
        if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
            try {
                tenantId = Long.parseLong(tenantIdHeader);
            } catch (NumberFormatException e) {
                log.warn("Invalid tenant ID header: {}", tenantIdHeader);
            }
        }
        request.setAttribute(TENANT_ID_KEY, tenantId);
        filterChain.doFilter(request, response);
    }
}
