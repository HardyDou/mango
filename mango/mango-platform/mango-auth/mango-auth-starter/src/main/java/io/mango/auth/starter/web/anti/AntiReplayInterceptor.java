package io.mango.auth.starter.web.anti;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.auth.api.AuthCode;
import io.mango.auth.core.anti.AppSecretProvider;
import io.mango.auth.core.anti.IdempotencyGuard;
import io.mango.auth.core.anti.ReplayGuard;
import io.mango.auth.core.anti.SignatureValidator;
import io.mango.common.result.R;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AppSecretProvider appSecretProvider;
    private final ObjectMapper objectMapper;

    /**
     * 本地密钥缓存：appKey -> secret。
     */
    private final Map<String, String> secretCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("AntiReplayInterceptor initialized");
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
                    writeError(response, HttpStatus.UNAUTHORIZED.value(), AuthCode.REQUEST_EXPIRED);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid timestamp header: {}", timestampStr);
                writeError(response, HttpStatus.BAD_REQUEST.value(), AuthCode.REQUEST_TIMESTAMP_INVALID);
                return false;
            }
        }

        // 2. 校验防重放 nonce。
        String nonce = request.getHeader(HEADER_NONCE);
        if (nonce != null && !nonce.isBlank()) {
            if (!replayGuard.tryAcquire(nonce)) {
                log.warn("Replay request rejected: nonce={}", nonce);
                writeError(response, HttpStatus.CONFLICT.value(), AuthCode.DUPLICATE_REQUEST);
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
            String secret = getSecretByAppKey(appKey);
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            String body = cachedRequest.getBody();
            boolean valid = signatureValidator.validate(signAlgo, appKey, secret,
                timestampStr != null ? timestampStr : "", body, sign);
            if (!valid) {
                log.warn("Signature validation failed: appKey={}", appKey);
                writeError(response, HttpStatus.UNAUTHORIZED.value(), AuthCode.REQUEST_SIGNATURE_INVALID);
                return false;
            }
        }

        return true;
    }

    private String getSecretByAppKey(String appKey) {
        if (appKey == null || appKey.isBlank()) {
            return "";
        }
        // 优先检查本地缓存。
        return secretCache.computeIfAbsent(appKey, this::loadSecret);
    }

    private void writeError(HttpServletResponse response, int httpStatus, AuthCode code) throws java.io.IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code)));
    }

    private String loadSecret(String appKey) {
        return appSecretProvider.findSecret(appKey);
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
