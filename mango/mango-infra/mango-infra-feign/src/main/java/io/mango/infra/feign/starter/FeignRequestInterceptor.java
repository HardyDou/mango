package io.mango.infra.feign.starter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feign request interceptor for context propagation
 * <p>
 * Propagates tenant ID and trace ID through Feign calls.
 * TODO: Integrate with TenantContextHolder and TraceContextHolder when implemented
 *
 * @author Mango
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    private static final String TENANT_HEADER = "TENANT-ID";
    private static final String TRACE_HEADER = "TRACE-ID";

    @Override
    public void apply(RequestTemplate template) {
        // Try to get tenant ID from context holder
        String tenantId = getTenantId();
        if (tenantId != null && !tenantId.isEmpty()) {
            template.header(TENANT_HEADER, tenantId);
            log.debug("Propagating tenant ID: {}", tenantId);
        }

        // Try to get trace ID from context holder
        String traceId = getTraceId();
        if (traceId != null && !traceId.isEmpty()) {
            template.header(TRACE_HEADER, traceId);
            log.debug("Propagating trace ID: {}", traceId);
        }
    }

    /**
     * Get tenant ID from context holder
     * TODO: Replace with TenantContextHolder.getTenantId() when available
     */
    private String getTenantId() {
        // Placeholder - when TenantContextHolder is implemented, use:
        // return TenantContextHolder.getTenantId();
        return System.getProperty("mango.tenant.id");
    }

    /**
     * Get trace ID from context holder
     * TODO: Replace with TraceContextHolder.getTraceId() when available
     */
    private String getTraceId() {
        // Placeholder - when TraceContextHolder is implemented, use:
        // return TraceContextHolder.getTraceId();
        return System.getProperty("mango.trace.id");
    }
}
