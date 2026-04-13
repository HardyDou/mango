package io.mango.auth.core.interceptor;

import io.mango.auth.core.anti.filter.AntiReplayInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for auth interceptors.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CaptchaInterceptor captchaInterceptor;
    private final AntiReplayInterceptor antiReplayInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Anti-replay interceptor - applies to all paths
        registry.addInterceptor(antiReplayInterceptor)
                .addPathPatterns("/**");

        // Captcha interceptor - only for configured paths
        // This is handled inside CaptchaInterceptor.isCaptchaRequired()
        registry.addInterceptor(captchaInterceptor)
                .addPathPatterns("/**");
    }
}
