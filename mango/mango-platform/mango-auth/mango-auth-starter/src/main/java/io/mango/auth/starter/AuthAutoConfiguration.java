package io.mango.auth.starter;

import io.mango.auth.api.AuthApi;
import io.mango.auth.core.anti.AppSecretProvider;
import io.mango.auth.core.service.IAuthService;
import io.mango.auth.core.service.impl.LoginAttemptTracker;
import io.mango.auth.starter.config.AuthSecurityProperties;
import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.web.anti.AntiReplayInterceptor;
import io.mango.auth.starter.web.anti.AntiReplayProperties;
import io.mango.auth.starter.web.anti.ConfiguredAppSecretProvider;
import io.mango.auth.starter.web.interceptor.CaptchaInterceptor;
import io.mango.auth.starter.web.interceptor.WebMvcConfig;
import io.mango.authorization.starter.autoconfigure.SecurityAutoConfiguration;
import io.mango.infra.kv.api.IKvStore;
import io.mango.system.api.SysConfigApi;
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
@AutoConfigureBefore(SecurityAutoConfiguration.class)
@EnableConfigurationProperties({AntiReplayProperties.class, AuthSecurityProperties.class})
@ComponentScan({
        "io.mango.auth.core.service",
        "io.mango.auth.core.service.impl",
        "io.mango.auth.core.config",
        "io.mango.auth.core.anti",
        "io.mango.auth.core.init",
        "io.mango.auth.starter.controller",
        "io.mango.auth.starter.notice",
        "io.mango.auth.starter.resource",
        "io.mango.auth.starter.web"
})
@Import(AuthSecurityConfig.class)
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AppSecretProvider appSecretProvider(AntiReplayProperties properties) {
        return new ConfiguredAppSecretProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean(AuthApi.class)
    public AuthApi authApi(IAuthService authService) {
        return new AuthApiAdapter(authService);
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    public LoginAttemptTracker loginAttemptTracker(IKvStore kvStore, ObjectProvider<SysConfigApi> sysConfigApiProvider) {
        SysConfigApi sysConfigApi = sysConfigApiProvider.getIfAvailable();
        int maxAttempts = integerConfig(sysConfigApi, "sys.login.lockCount", 5);
        long failureWindowMinutes = integerConfig(sysConfigApi, "identity.security.login.failure-window-minutes", 60);
        long lockDurationMinutes = integerConfig(sysConfigApi, "identity.security.login.lock-duration-minutes", 15);
        return new LoginAttemptTracker(kvStore, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "auth-login-attempt-cleanup");
            thread.setDaemon(true);
            return thread;
        }), maxAttempts, failureWindowMinutes, lockDurationMinutes);
    }

    private int integerConfig(SysConfigApi sysConfigApi, String key, int defaultValue) {
        if (sysConfigApi == null) {
            return defaultValue;
        }
        try {
            var result = sysConfigApi.getIntegerValue(key, defaultValue);
            return result.isSuccess() && result.getData() != null ? result.getData() : defaultValue;
        } catch (RuntimeException ex) {
            return defaultValue;
        }
    }

    @Bean
    public WebMvcConfig webMvcConfig(CaptchaInterceptor captchaInterceptor,
                                      AntiReplayInterceptor antiReplayInterceptor) {
        return new WebMvcConfig(captchaInterceptor, antiReplayInterceptor);
    }
}
