package io.mango.auth.starter;

import io.mango.auth.core.anti.AppSecretProvider;
import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.web.anti.AntiReplayInterceptor;
import io.mango.auth.starter.web.anti.AntiReplayProperties;
import io.mango.auth.starter.web.anti.ConfiguredAppSecretProvider;
import io.mango.auth.starter.web.interceptor.CaptchaInterceptor;
import io.mango.auth.starter.web.interceptor.WebMvcConfig;
import io.mango.authorization.support.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证服务自动配置。
 *
 * @author hardy
 */
@AutoConfiguration
@AutoConfigureBefore(SecurityAutoConfiguration.class)
@EnableConfigurationProperties(AntiReplayProperties.class)
@ComponentScan({
        "io.mango.auth.core.service",
        "io.mango.auth.core.service.impl",
        "io.mango.auth.core.config",
        "io.mango.auth.core.anti",
        "io.mango.auth.core.init",
        "io.mango.auth.starter.controller",
        "io.mango.auth.starter.web"
})
@Import(AuthSecurityConfig.class)
public class AuthAutoConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public AppSecretProvider appSecretProvider(AntiReplayProperties properties) {
        return new ConfiguredAppSecretProvider(properties);
    }

    @Bean
    public WebMvcConfig webMvcConfig(CaptchaInterceptor captchaInterceptor,
                                      AntiReplayInterceptor antiReplayInterceptor) {
        return new WebMvcConfig(captchaInterceptor, antiReplayInterceptor);
    }
}
