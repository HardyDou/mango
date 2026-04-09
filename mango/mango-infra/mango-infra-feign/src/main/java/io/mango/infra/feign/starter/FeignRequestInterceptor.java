package io.mango.infra.feign.starter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.mango.infra.context.core.TenantContextHolder;
import io.mango.infra.context.core.TraceContextHolder;
import io.mango.common.context.TokenContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign request interceptor for context propagation.
 * <p>
 * Propagates tenant ID, trace ID, and JWT token through Feign calls.
 *
 * @author Mango
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    private static final String TENANT_HEADER = "TENANT-ID";
    private static final String TRACE_HEADER = "TRACE-ID";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        // Propagate JWT token
        String token = TokenContextHolder.getToken();
        if (token != null && !token.isEmpty()) {
            template.header(AUTHORIZATION_HEADER, token);
            log.debug("Propagating JWT token");
        }

        // Propagate tenant ID
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            template.header(TENANT_HEADER, tenantId);
            log.debug("Propagating tenant ID: {}", tenantId);
        }

        // Propagate trace ID
        String traceId = TraceContextHolder.getTraceId();
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TRACE_HEADER, traceId);
            log.debug("Propagating trace ID: {}", traceId);
        }
    }
}
