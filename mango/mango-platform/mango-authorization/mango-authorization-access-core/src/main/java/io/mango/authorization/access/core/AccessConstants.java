package io.mango.authorization.access.core;

/**
 * 网关内部常量。
 *
 * @author Mango
 */
public final class AccessConstants {

    /**
     * Token 请求头名称。
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * 默认匿名访问路径。
     */
    public static final String[] DEFAULT_ANONYMOUS_PATHS = {
            "/auth/login",
            "/auth/refresh",
            "/auth/captcha",
            "/captcha/**",
            "/kaptcha/**",
            "/actuator/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    private AccessConstants() {
    }
}
