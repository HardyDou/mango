package io.mango.gateway.api;

/**
 * Gateway常量定义
 *
 * @author Mango
 */
public class GatewayConstant {

    /**
     * 用户ID在请求头中的Key
     */
    public static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 租户ID在请求头中的Key
     */
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    /**
     * Token在请求头中的Key
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * 绕过认证的路径（白名单）
     */
    public static final String[] WHITE_LIST = {
            // 认证相关
            "/auth/login",
            "/auth/refresh",
            "/auth/captcha",
            // 验证码公开接口
            "/captcha/**",
            "/kaptcha/**",
            // 健康检查
            "/actuator/health",
            // API文档
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };
}
