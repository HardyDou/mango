package io.mango.auth.core.constant;

/**
 * 认证常量。
 *
 * @author Mango
 */
public class AuthConstant {

    /**
     * 令牌请求头名称。
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * 认证白名单，不需要认证的路径。
     */
    public static final String[] WHITE_LIST = {
            // 认证接口
            "/auth/login",
            "/auth/refresh",
            "/auth/captcha",
            // 验证码
            "/captcha/**",
            "/kaptcha/**",
            // 健康检查
            "/actuator/health",
            // API 文档
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    private AuthConstant() {
    }
}
