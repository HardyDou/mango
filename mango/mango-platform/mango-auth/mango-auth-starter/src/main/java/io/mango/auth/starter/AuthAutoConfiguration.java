package io.mango.auth.starter;

import io.mango.auth.starter.config.AuthSecurityConfig;
import io.mango.auth.starter.web.anti.AntiReplayInterceptor;
import io.mango.auth.starter.web.interceptor.CaptchaInterceptor;
import io.mango.auth.starter.web.interceptor.WebMvcConfig;
import io.mango.infra.security.starter.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
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
    public WebMvcConfig webMvcConfig(CaptchaInterceptor captchaInterceptor,
                                      AntiReplayInterceptor antiReplayInterceptor) {
        return new WebMvcConfig(captchaInterceptor, antiReplayInterceptor);
    }
}
