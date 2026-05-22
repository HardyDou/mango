package io.mango.auth.starter.web.interceptor;

import io.mango.auth.api.AuthCode;
import io.mango.auth.api.spi.CaptchaConfigService;
import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.result.R;
import io.mango.infra.context.core.MangoContextHolder;
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
 * 验证码拦截器，用于防止暴力破解。
 *
 * 处理流程：
 * 1. 查询 CaptchaConfigService 判断当前路径是否需要验证码。
 * 2. 路径未配置时按 fail-open 处理，直接放行。
 * 3. 不需要验证码时直接放行。
 * 4. 需要验证码时检查 X-Captcha-Key / X-Captcha-Code 请求头。
 * 5. 未携带验证码请求头时返回 HTTP 428。
 * 6. 携带验证码请求头时调用 captchaApi.verify() 校验。
 * 7. 校验失败时返回 HTTP 400。
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

    // 按 IP 追踪验证码失败次数，用于限流。
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ip = resolveClientIp(request);

        // 检查当前路径是否需要验证码。
        if (!captchaConfigService.isCaptchaRequired(path)) {
            return true; // 未配置时 fail-open，直接放行。
        }

        // 失败次数过多时锁定。
        if (isLockedOut(ip)) {
            log.warn("IP locked out due to too many captcha failures: ip={}", ip);
            sendError(response, HttpStatus.TOO_MANY_REQUESTS.value(), AuthCode.LOGIN_ATTEMPT_LOCKED.getCode(),
                    "登录尝试次数过多，请稍后再试");
            return false;
        }

        String captchaKey = request.getHeader(HEADER_CAPTCHA_KEY);
        String captchaCode = request.getHeader(HEADER_CAPTCHA_CODE);
        String captchaType = request.getHeader(HEADER_CAPTCHA_TYPE);

        // 未携带验证码请求头。
        if (captchaKey == null || captchaKey.isBlank()) {
            log.info("Captcha required but headers missing: path={}, ip={}", path, ip);
            sendError(response, 428, AuthCode.CAPTCHA_REQUIRED.getCode(), AuthCode.CAPTCHA_REQUIRED.getMessage());
            return false;
        }

        // 校验验证码。
        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setKey(captchaKey);
        verifyRequest.setCode(captchaCode);
        verifyRequest.setType(io.mango.captcha.api.constant.CaptchaType.valueOf(
            captchaType != null ? captchaType : "ARITHMETIC"));

        boolean verified = captchaApi.verify(verifyRequest);
        if (!verified) {
            incrementFailedAttempts(ip);
            log.warn("Captcha verification failed: ip={}, path={}", ip, path);
            sendError(response, HttpStatus.BAD_REQUEST.value(), AuthCode.CAPTCHA_INVALID.getCode(), AuthCode.CAPTCHA_INVALID.getMessage());
            return false;
        }

        // 校验成功后清理失败次数。
        failedAttempts.remove(ip);
        return true;
    }

    private boolean isLockedOut(String ip) {
        AtomicInteger count = failedAttempts.get(ip);
        return count != null && count.get() >= 5; // 失败 5 次后锁定。
    }

    private void incrementFailedAttempts(String ip) {
        AtomicInteger count = failedAttempts.computeIfAbsent(ip, k -> new AtomicInteger(0));
        int failures = count.incrementAndGet();
        log.info("Captcha failure count: ip={}, count={}", ip, failures);
    }

    private void sendError(HttpServletResponse response, int status, int code, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        R<?> r = R.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(r));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String clientIp = MangoContextHolder.clientIp();
        if (clientIp != null) {
            return clientIp;
        }
        return request.getRemoteAddr();
    }
}
