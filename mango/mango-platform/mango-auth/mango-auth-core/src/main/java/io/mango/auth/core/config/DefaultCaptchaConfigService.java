package io.mango.auth.core.config;

import io.mango.auth.api.spi.CaptchaConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 默认验证码配置服务。
 * fail-open 策略：路径未配置时不要求验证码。
 */
@Slf4j
@Component
public class DefaultCaptchaConfigService implements CaptchaConfigService {

    @Value("${mango.captcha.required-paths:/login,/register}")
    private Set<String> requiredPaths;

    @Value("${mango.captcha.ttl:300}")
    private long defaultTtl;

    @Override
    public boolean isCaptchaRequired(String path) {
        if (path == null) {
            return false;
        }
        return requiredPaths.stream().anyMatch(path::contains);
    }

    @Override
    public String getCaptchaType(String path) {
        return "default";
    }

    @Override
    public long getCaptchaTtl(String path) {
        return defaultTtl;
    }
}
