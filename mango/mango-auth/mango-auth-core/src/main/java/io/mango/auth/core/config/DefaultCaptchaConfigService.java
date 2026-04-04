package io.mango.auth.core.config;

import io.mango.auth.api.spi.CaptchaConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Default captcha configuration service.
 * Fail-open: if path not configured, captcha is not required.
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
