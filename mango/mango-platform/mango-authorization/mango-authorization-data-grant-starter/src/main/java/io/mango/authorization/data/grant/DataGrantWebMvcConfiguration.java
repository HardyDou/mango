package io.mango.authorization.data.grant;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把 {@link DataGrantInterceptor} 注册到所有路径。
 * <p>
 * 拦截器自身只处理带 {@code @DataGrant} 且能解析出单条记录 ID 的端点，因此全局注册不会影响其它接口。
 */
class DataGrantWebMvcConfiguration implements WebMvcConfigurer {

    private final DataGrantInterceptor interceptor;

    /**
     * 构造注册配置。
     *
     * @param interceptor Grant 拦截器。
     */
    DataGrantWebMvcConfiguration(DataGrantInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**");
    }
}
