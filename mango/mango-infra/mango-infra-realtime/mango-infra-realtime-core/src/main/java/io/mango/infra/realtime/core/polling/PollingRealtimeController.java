package io.mango.infra.realtime.core.polling;

import io.mango.infra.realtime.api.RealtimeHeaders;
import io.mango.infra.realtime.api.RealtimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PollingRealtimeController {

    private final InMemoryRealtimePollingService pollingService;
    private final int defaultMaxSize;
    private final int maxSize;
    private final long defaultTimeoutMillis;
    private final long maxTimeoutMillis;

    @GetMapping("${mango.infra.realtime.polling.endpoint:/realtime/poll}")
    public DeferredResult<List<RealtimeMessage>> poll(
            @RequestHeader(value = RealtimeHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = RealtimeHeaders.TENANT_ID, required = false) String tenantId,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "maxSize", required = false) Integer requestedMaxSize,
            @RequestParam(value = "timeoutMillis", required = false) Long requestedTimeoutMillis) {

        // Full token validation belongs to gateway/security; realtime only rejects malformed protocol entry.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("Polling request rejected: missing or invalid Authorization header");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String subscriberId = InMemoryRealtimePollingService.userSubscriberId(userId);
        int effectiveMaxSize = normalizeMaxSize(requestedMaxSize);
        long effectiveTimeoutMillis = normalizeTimeoutMillis(requestedTimeoutMillis);
        return pollingService.pollAsync(subscriberId, normalizeTenantId(tenantId), effectiveMaxSize, effectiveTimeoutMillis);
    }

    private int normalizeMaxSize(Integer requestedMaxSize) {
        int value = requestedMaxSize == null || requestedMaxSize <= 0 ? defaultMaxSize : requestedMaxSize;
        return Math.min(value, maxSize);
    }

    private long normalizeTimeoutMillis(Long requestedTimeoutMillis) {
        long value = requestedTimeoutMillis == null || requestedTimeoutMillis < 0
                ? defaultTimeoutMillis
                : requestedTimeoutMillis;
        return Math.min(value, maxTimeoutMillis);
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId;
    }
}
