package io.mango.auth.starter;

import io.mango.auth.core.anti.filter.AntiReplayInterceptor;
import io.mango.auth.core.interceptor.CaptchaInterceptor;
import io.mango.auth.core.interceptor.WebMvcConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auth service auto configuration
 *
 * @author Mango
 */
@Configuration
@MapperScan("io.mango.auth.core.mapper")
@ComponentScan({
        "io.mango.auth.core.service",
        "io.mango.auth.core.service.impl",
        "io.mango.auth.core.anti",
        "io.mango.auth.core.anti.filter",
        "io.mango.auth.core.interceptor",
        "io.mango.auth.core.init",
        "io.mango.auth.starter.controller"
})
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
