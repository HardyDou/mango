package io.mango.auth.starter;

import io.mango.auth.core.anti.AppSecretProvider;
import io.mango.auth.api.spi.CaptchaConfigService;
import io.mango.auth.core.config.DefaultCaptchaConfigService;
import io.mango.auth.core.service.impl.LoginAttemptTracker;
import io.mango.auth.starter.config.AuthSecurityProperties;
import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.web.anti.AntiReplayProperties;
import io.mango.auth.starter.web.anti.ConfiguredAppSecretProvider;
import io.mango.auth.starter.web.interceptor.CaptchaInterceptor;
import io.mango.auth.starter.web.interceptor.WebMvcConfig;
import io.mango.infra.kv.api.IKvStore;
import io.mango.system.api.spi.SystemConfigProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.Executors;

/**
 * 认证服务自动配置。
 *
 * @author hardy
 */
@AutoConfiguration
@AutoConfigureBefore(name = "io.mango.authorization.starter.autoconfigure.SecurityAutoConfiguration")
@EnableConfigurationProperties({AntiReplayProperties.class, AuthSecurityProperties.class})
@ComponentScan({
    "io.mango.auth.core.service",
    "io.mango.auth.core.service.impl",
    "io.mango.auth.core.config",
    "io.mango.auth.core.store",
    "io.mango.auth.core.anti",
    "io.mango.auth.core.init",
    "io.mango.auth.starter.controller",
    "io.mango.auth.starter.notice",
    "io.mango.auth.starter.resource",
    "io.mango.auth.starter.web"
})
@Import(AuthSecurityConfig.class)
public class AuthAutoConfiguration {

    private static final int DEFAULT_MAX_LOGIN_ATTEMPTS = 5;
    private static final int DEFAULT_FAILURE_WINDOW_MINUTES = 60;
    private static final int DEFAULT_LOCK_DURATION_MINUTES = 15;

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(CaptchaConfigService.class)
    public DefaultCaptchaConfigService captchaConfigService() {
        return new DefaultCaptchaConfigService();
    }

    @Bean
    @ConditionalOnMissingBean
    public AppSecretProvider appSecretProvider(AntiReplayProperties properties) {
        return new ConfiguredAppSecretProvider(properties);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public LoginAttemptTracker loginAttemptTracker(IKvStore kvStore, ObjectProvider<SystemConfigProvider> configProvider) {
        SystemConfigProvider config = configProvider.getIfAvailable();
        int maxAttempts = integerConfig(config, "sys.login.lockCount", DEFAULT_MAX_LOGIN_ATTEMPTS);
        long failureWindowMinutes = integerConfig(config, "identity.security.login.failure-window-minutes",
                DEFAULT_FAILURE_WINDOW_MINUTES);
        long lockDurationMinutes = integerConfig(config, "identity.security.login.lock-duration-minutes",
                DEFAULT_LOCK_DURATION_MINUTES);
        return new LoginAttemptTracker(kvStore, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "auth-login-attempt-cleanup");
            thread.setDaemon(true);
            return thread;
        }), maxAttempts, failureWindowMinutes, lockDurationMinutes);
    }

    private int integerConfig(SystemConfigProvider config, String key, int defaultValue) {
        if (config == null) {
            return defaultValue;
        }
        try {
            Integer result = config.getIntegerValue(key, defaultValue);
            if (result == null) {
                return defaultValue;
            }
            return result;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    @Bean
    public WebMvcConfig webMvcConfig(CaptchaInterceptor captchaInterceptor) {
        return new WebMvcConfig(captchaInterceptor);
    }
}
