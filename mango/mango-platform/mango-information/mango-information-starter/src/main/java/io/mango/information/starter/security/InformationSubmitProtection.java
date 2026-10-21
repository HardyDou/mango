package io.mango.information.starter.security;

import io.mango.captcha.api.CaptchaApi;
import io.mango.captcha.api.constant.CaptchaType;
import io.mango.captcha.api.dto.CaptchaVerifyRequest;
import io.mango.common.exception.BizException;
import io.mango.common.result.R;
import io.mango.information.api.enums.InformationCode;
import io.mango.information.starter.config.InformationSubmitProtectionProperties;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.kv.api.IKvStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 公开表单提交的风险触发式防刷保护。
 *
 * <p>正常请求只增加共享 KV 固定窗口计数；达到风险阈值后才校验一次性验证码，
 * 超过硬上限时在进入提交事务前返回 429。</p>
 */
@Slf4j
public class InformationSubmitProtection {

    public static final String HEADER_CAPTCHA_KEY = "X-Captcha-Key";
    public static final String HEADER_CAPTCHA_CODE = "X-Captcha-Code";
    public static final String HEADER_CAPTCHA_TYPE = "X-Captcha-Type";
    public static final String HEADER_CAPTCHA_POINT = "X-Captcha-Point";

    private static final int ALLOWED_STATUS = HttpStatus.OK.value();
    private static final int CAPTCHA_REQUIRED_STATUS = HttpStatus.PRECONDITION_REQUIRED.value();
    private static final int CAPTCHA_INVALID_STATUS = HttpStatus.BAD_REQUEST.value();
    private static final int RATE_LIMITED_STATUS = HttpStatus.TOO_MANY_REQUESTS.value();
    private static final int PROTECTION_UNAVAILABLE_STATUS = HttpStatus.SERVICE_UNAVAILABLE.value();
    private static final String KEY_PREFIX = "information:public-submit:v1:";

    private final ObjectProvider<IKvStore> kvStoreProvider;
    private final ObjectProvider<CaptchaApi> captchaApiProvider;
    private final InformationSubmitProtectionProperties properties;

    public InformationSubmitProtection(ObjectProvider<IKvStore> kvStoreProvider,
            ObjectProvider<CaptchaApi> captchaApiProvider,
            InformationSubmitProtectionProperties properties) {
        this.kvStoreProvider = kvStoreProvider;
        this.captchaApiProvider = captchaApiProvider;
        this.properties = copyProperties(properties);
    }

    public Decision check(HttpServletRequest request, String formCode) {
        if (!properties.isEnabled()) {
            return Decision.allow();
        }

        String clientIp = resolveClientIp(request);
        String key = buildKey(clientIp, formCode);
        IKvStore kvStore = kvStoreProvider.getIfAvailable();
        if (kvStore == null) {
            return storageUnavailable("公开提交防刷依赖未配置");
        }

        long count;
        try {
            count = kvStore.increment(key, properties.getWindowSeconds());
        } catch (RuntimeException exception) {
            log.warn("公开提交防刷计数失败，failOpen={}", properties.isFailOpen(), exception);
            return storageUnavailable("公开提交防刷依赖暂不可用");
        }

        if (count > properties.getMaxRequests()) {
            return Decision.rateLimited(properties.getWindowSeconds());
        }
        if (count < properties.getRiskThreshold()) {
            return Decision.allow();
        }
        return verifyCaptcha(request);
    }

    private Decision verifyCaptcha(HttpServletRequest request) {
        String key = request.getHeader(HEADER_CAPTCHA_KEY);
        if (!StringUtils.hasText(key)) {
            return Decision.captchaRequired();
        }

        CaptchaApi captchaApi = captchaApiProvider.getIfAvailable();
        if (captchaApi == null) {
            return Decision.protectionUnavailable("验证码服务暂不可用");
        }

        CaptchaVerifyRequest verifyRequest = new CaptchaVerifyRequest();
        verifyRequest.setKey(key);
        verifyRequest.setCode(request.getHeader(HEADER_CAPTCHA_CODE));
        verifyRequest.setPointJson(request.getHeader(HEADER_CAPTCHA_POINT));
        String type = request.getHeader(HEADER_CAPTCHA_TYPE);
        if (StringUtils.hasText(type)) {
            try {
                verifyRequest.setType(CaptchaType.valueOf(type.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                return Decision.captchaInvalid();
            }
        }

        try {
            R<Boolean> result = captchaApi.verify(verifyRequest);
            if (result != null && result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                return Decision.allow();
            }
            return Decision.captchaInvalid();
        } catch (BizException exception) {
            return Decision.captchaInvalid();
        } catch (RuntimeException exception) {
            log.warn("公开提交验证码服务调用失败", exception);
            return Decision.protectionUnavailable("验证码服务暂不可用");
        }
    }

    private Decision storageUnavailable(String message) {
        return properties.isFailOpen() ? Decision.allow() : Decision.protectionUnavailable(message);
    }

    private InformationSubmitProtectionProperties copyProperties(InformationSubmitProtectionProperties source) {
        InformationSubmitProtectionProperties copy = new InformationSubmitProtectionProperties();
        copy.setEnabled(source.isEnabled());
        copy.setWindowSeconds(source.getWindowSeconds());
        copy.setRiskThreshold(source.getRiskThreshold());
        copy.setMaxRequests(source.getMaxRequests());
        copy.setKeyDimension(source.getKeyDimension());
        copy.setFailOpen(source.isFailOpen());
        return copy;
    }

    private String buildKey(String clientIp, String formCode) {
        String dimension = properties.getKeyDimension() == InformationSubmitProtectionProperties.KeyDimension.IP
                ? clientIp
                : clientIp + ":" + formCode;
        return KEY_PREFIX + digest(dimension);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String contextIp = MangoContextHolder.clientIp();
        return StringUtils.hasText(contextIp) ? contextIp : request.getRemoteAddr();
    }

    private String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Decision(boolean allowed, int status, InformationCode code, String message,
            long retryAfterSeconds) {
        static Decision allow() {
            return new Decision(true, ALLOWED_STATUS, null, null, 0);
        }

        static Decision captchaRequired() {
            return new Decision(false, CAPTCHA_REQUIRED_STATUS, InformationCode.SUBMISSION_CAPTCHA_REQUIRED,
                    InformationCode.SUBMISSION_CAPTCHA_REQUIRED.getMessage(), 0);
        }

        static Decision captchaInvalid() {
            return new Decision(false, CAPTCHA_INVALID_STATUS, InformationCode.SUBMISSION_CAPTCHA_INVALID,
                    InformationCode.SUBMISSION_CAPTCHA_INVALID.getMessage(), 0);
        }

        static Decision rateLimited(long retryAfterSeconds) {
            return new Decision(false, RATE_LIMITED_STATUS, InformationCode.SUBMISSION_RATE_LIMITED,
                    InformationCode.SUBMISSION_RATE_LIMITED.getMessage(), retryAfterSeconds);
        }

        static Decision protectionUnavailable(String message) {
            return new Decision(false, PROTECTION_UNAVAILABLE_STATUS,
                    InformationCode.SUBMISSION_PROTECTION_UNAVAILABLE, message, 0);
        }
    }
}
