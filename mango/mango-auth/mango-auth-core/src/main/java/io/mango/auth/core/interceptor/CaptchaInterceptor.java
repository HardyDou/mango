package io.mango.auth.core.interceptor;

import io.mango.auth.api.spi.CaptchaConfigService;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.result.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captcha interceptor for brute-force protection.
 *
 * Behavior:
 * 1. Query CaptchaConfigService.getConfig(path) — is captcha required?
 * 2. path is nil or not configured → fail-open (allow, no captcha)
 * 3. Not required → allow
 * 4. Required → check X-Captcha-Key / X-Captcha-Code headers
 * 5. No header → return HTTP 428
 * 6. Has header → captchaApi.verify() validation
 * 7. Failed → return HTTP 400
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CaptchaInterceptor implements HandlerInterceptor {

    private static final String HEADER_CAPTCHA_KEY = "X-Captcha-Key";
    private static final String HEADER_CAPTCHA_CODE = "X-Captcha-Code";
    private static final String HEADER_CAPTCHA_TYPE = "X-Captcha-Type";

    private final CaptchaConfigService captchaConfigService;
    private final CaptchaApi captchaApi;
    private final ObjectMapper objectMapper;

    // Track failed attempts per IP for rate limiting
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ip = getClientIp(request);

        // Check if captcha is required for this path
        if (!captchaConfigService.isCaptchaRequired(path)) {
            return true; // fail-open: allow if not configured
        }

        // Check if already locked due to too many failures
        if (isLockedOut(ip)) {
            log.warn("IP locked out due to too many captcha failures: ip={}", ip);
            sendError(response, HttpStatus.TOO_MANY_REQUESTS.value(), "Too many failed attempts, please try again later");
            return false;
        }

        String captchaKey = request.getHeader(HEADER_CAPTCHA_KEY);
        String captchaCode = request.getHeader(HEADER_CAPTCHA_CODE);
        String captchaType = request.getHeader(HEADER_CAPTCHA_TYPE);

        // No captcha headers provided
        if (captchaKey == null || captchaKey.isBlank()) {
            log.info("Captcha required but headers missing: path={}, ip={}", path, ip);
            sendError(response, 428, "Captcha Required");
            return false;
        }

        // Verify captcha
        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setKey(captchaKey);
        verifyRequest.setCode(captchaCode);
        verifyRequest.setType(io.mango.captcha.api.constant.CaptchaType.valueOf(
            captchaType != null ? captchaType : "ARITHMETIC"));

        boolean verified = captchaApi.verify(verifyRequest);
        if (!verified) {
            incrementFailedAttempts(ip);
            log.warn("Captcha verification failed: ip={}, path={}", ip, path);
            sendError(response, HttpStatus.BAD_REQUEST.value(), "Invalid captcha");
            return false;
        }

        // Reset failed attempts on success
        failedAttempts.remove(ip);
        return true;
    }

    private boolean isLockedOut(String ip) {
        AtomicInteger count = failedAttempts.get(ip);
        return count != null && count.get() >= 5; // lock after 5 failures
    }

    private void incrementFailedAttempts(String ip) {
        AtomicInteger count = failedAttempts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int failures = count.incrementAndGet();
        log.info("Captcha failure count: ip={}, count={}", ip, failures);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        R<?> r = R.fail(message);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }
}
