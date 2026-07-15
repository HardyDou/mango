package io.mango.auth.starter.web.anti;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.mango.auth.core.anti.AppSecretProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 基于配置的应用签名密钥提供者。
 */
@Slf4j
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "The configuration object is an intentionally shared Spring Boot binding target")
public class ConfiguredAppSecretProvider implements AppSecretProvider {

    private final AntiReplayProperties properties;

    @Override
    public String findSecret(String appKey) {
        if (!StringUtils.hasText(appKey)) {
            return null;
        }
        String secret = properties.getAppSecrets().get(appKey);
        if (StringUtils.hasText(secret)) {
            return secret;
        }
        if (properties.isAllowFallback() && StringUtils.hasText(properties.getDefaultSecret())) {
            log.warn("使用默认签名密钥兜底: appKey={}", appKey);
            return properties.getDefaultSecret();
        }
        log.warn("未找到应用签名密钥: appKey={}", appKey);
        return null;
    }
}
