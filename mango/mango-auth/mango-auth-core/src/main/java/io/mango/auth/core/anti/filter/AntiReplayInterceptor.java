package io.mango.auth.core.anti.filter;

import io.mango.auth.core.anti.IdempotencyGuard;
import io.mango.auth.core.anti.ReplayGuard;
import io.mango.auth.core.anti.SignatureValidator;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti-replay interceptor for request validation.
 * Validates:
 * 1. X-Request-Timestamp - must be within 5 minutes
 * 2. X-Replay-Nonce - must be unique (not replayed)
 * 3. X-Idempotency-Key - for POST/PUT/DELETE requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AntiReplayInterceptor implements HandlerInterceptor {

    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000; // 5 minutes
    private static final String HEADER_TIMESTAMP = "X-Request-Timestamp";
    private static final String HEADER_NONCE = "X-Replay-Nonce";
    private static final String HEADER_IDEM_KEY = "X-Idempotency-Key";
    private static final String HEADER_SIGN_ALGO = "X-Sign-Algorithm";
    private static final String HEADER_APP_KEY = "X-App-Key";
    private static final String HEADER_SIGN = "X-Sign";

    private final ReplayGuard replayGuard;
    private final IdempotencyGuard idempotencyGuard;
    private final SignatureValidator signatureValidator;

    @Value("${mango.auth.app-secret.default:#{null}}")
    private String defaultSecret;

    @Value("${mango.auth.app-secret.allow-fallback:false}")
    private boolean allowFallback;

    /**
     * Local cache for secrets: appKey -> secret
     */
    private final Map<String, String> secretCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("AntiReplayInterceptor initialized: allowFallback={}, hasDefaultSecret={}",
            allowFallback, defaultSecret != null);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String method = request.getMethod();

        // 1. Validate timestamp
        String timestampStr = request.getHeader(HEADER_TIMESTAMP);
        if (timestampStr != null && !timestampStr.isBlank()) {
            try {
                long timestamp = Long.parseLong(timestampStr);
                long now = System.currentTimeMillis();
                if (Math.abs(now - timestamp) > MAX_TIMESTAMP_DIFF_MS) {
                    log.warn("Request timestamp expired: timestamp={}, now={}", timestamp, now);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write("{\"code\":401,\"msg\":\"Request expired\"}");
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid timestamp header: {}", timestampStr);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                response.getWriter().write("{\"code\":400,\"msg\":\"Invalid timestamp\"}");
                return false;
            }
        }

        // 2. Validate replay nonce
        String nonce = request.getHeader(HEADER_NONCE);
        if (nonce != null && !nonce.isBlank()) {
            if (!replayGuard.tryAcquire(nonce)) {
                log.warn("Replay request rejected: nonce={}", nonce);
                response.setStatus(HttpStatus.CONFLICT.value());
                response.getWriter().write("{\"code\":409,\"msg\":\"Duplicate request\"}");
                return false;
            }
        }

        // 3. Validate idempotency key for write operations
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            String idemKey = request.getHeader(HEADER_IDEM_KEY);
            if (idemKey != null && !idemKey.isBlank()) {
                if (!idempotencyGuard.tryAcquire(idemKey)) {
                    // Return cached response
                    String cached = idempotencyGuard.getResponse(idemKey);
                    if (cached != null) {
                        log.info("Returning cached response for idempotent key: {}", idemKey);
                        response.setStatus(HttpStatus.OK.value());
                        response.setContentType("application/json");
                        response.getWriter().write(cached);
                        return false;
                    }
                }
            }
        }

        // 4. Validate signature (if provided)
        String signAlgo = request.getHeader(HEADER_SIGN_ALGO);
        String appKey = request.getHeader(HEADER_APP_KEY);
        String sign = request.getHeader(HEADER_SIGN);
        if (signAlgo != null && appKey != null && sign != null) {
            // In production, fetch secret from DB/cache by appKey
            String secret = getSecretByAppKey(appKey);
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            String body = cachedRequest.getBody();
            boolean valid = signatureValidator.validate(signAlgo, appKey, secret,
                timestampStr != null ? timestampStr : "", body, sign);
            if (!valid) {
                log.warn("Signature validation failed: appKey={}", appKey);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("{\"code\":401,\"msg\":\"Invalid signature\"}");
                return false;
            }
        }

        return true;
    }

    private String getSecretByAppKey(String appKey) {
        if (appKey == null || appKey.isBlank()) {
            throw new IllegalArgumentException("appKey cannot be null or blank");
        }
        // Check local cache first
        return secretCache.computeIfAbsent(appKey, this::loadSecret);
    }

    private String loadSecret(String appKey) {
        // TODO: In production, fetch from DB/config service via AppSecretService
        // For now, use default secret only when explicitly allowed and configured
        if (allowFallback && defaultSecret != null) {
            log.warn("Using default secret as fallback for appKey={}. This should NOT happen in production!", appKey);
            return defaultSecret;
        }
        log.warn("Unknown appKey={} rejected in production mode (allowFallback=false)", appKey);
        return null;
    }

    /**
     * Simple cached body request wrapper to allow reading body multiple times.
     */
    @Getter
    @Slf4j
    public static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final String body;

        public CachedBodyHttpServletRequest(HttpServletRequest request) {
            super(request);
            String bodyStr;
            try {
                bodyStr = request.getReader().lines().reduce("", String::concat);
            } catch (java.io.IOException e) {
                bodyStr = "";
            }
            this.body = bodyStr;
        }

    }
}
