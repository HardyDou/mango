package io.mango.auth.starter.web.interceptor;

import io.mango.auth.starter.web.anti.AntiReplayInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 认证拦截器 Web MVC 配置。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CaptchaInterceptor captchaInterceptor;
    private final AntiReplayInterceptor antiReplayInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 防重放拦截器作用于所有路径。
        registry.addInterceptor(antiReplayInterceptor)
                .addPathPatterns("/**");

        // 验证码拦截器作用于所有路径，是否启用由 CaptchaInterceptor.isCaptchaRequired() 决定。
        registry.addInterceptor(captchaInterceptor)
                .addPathPatterns("/**");
    }
}
