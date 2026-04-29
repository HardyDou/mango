package io.mango.auth.starter.web.anti;

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
 * 防重放拦截器，用于请求校验。
 * 校验内容：
 * 1. X-Request-Timestamp 必须在 5 分钟内。
 * 2. X-Replay-Nonce 必须唯一，不能重复提交。
 * 3. X-Idempotency-Key 用于 POST/PUT/DELETE 请求幂等控制。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AntiReplayInterceptor implements HandlerInterceptor {

    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000; // 5 分钟
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
     * 本地密钥缓存：appKey -> secret。
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

        // 1. 校验时间戳。
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

        // 2. 校验防重放 nonce。
        String nonce = request.getHeader(HEADER_NONCE);
        if (nonce != null && !nonce.isBlank()) {
            if (!replayGuard.tryAcquire(nonce)) {
                log.warn("Replay request rejected: nonce={}", nonce);
                response.setStatus(HttpStatus.CONFLICT.value());
                response.getWriter().write("{\"code\":409,\"msg\":\"Duplicate request\"}");
                return false;
            }
        }

        // 3. 写操作校验幂等键。
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            String idemKey = request.getHeader(HEADER_IDEM_KEY);
            if (idemKey != null && !idemKey.isBlank()) {
                if (!idempotencyGuard.tryAcquire(idemKey)) {
                    // 返回缓存响应。
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

        // 4. 携带签名时校验签名。
        String signAlgo = request.getHeader(HEADER_SIGN_ALGO);
        String appKey = request.getHeader(HEADER_APP_KEY);
        String sign = request.getHeader(HEADER_SIGN);
        if (signAlgo != null && appKey != null && sign != null) {
            // 生产环境按 appKey 从数据库或缓存读取密钥。
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
        // 优先检查本地缓存。
        return secretCache.computeIfAbsent(appKey, this::loadSecret);
    }

    private String loadSecret(String appKey) {
        // TODO: 生产环境通过 AppSecretService 从数据库或配置中心读取。
        // 当前仅在显式允许且已配置时使用默认密钥兜底。
        if (allowFallback && defaultSecret != null) {
            log.warn("Using default secret as fallback for appKey={}. This should NOT happen in production!", appKey);
            return defaultSecret;
        }
        log.warn("Unknown appKey={} rejected in production mode (allowFallback=false)", appKey);
        return null;
    }

    /**
     * 简单的请求体缓存包装器，允许多次读取请求体。
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
