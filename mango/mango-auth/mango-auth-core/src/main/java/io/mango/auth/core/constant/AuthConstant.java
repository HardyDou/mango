package io.mango.auth.core.constant;

/**
 * Authentication constants
 *
 * @author Mango
 */
public class AuthConstant {

    /**
     * Token header name
     */
    public static final String TOKEN_HEADER = "Authorization";

    /**
     * White list for authentication - paths that don't require authentication
     */
    public static final String[] WHITE_LIST = {
            // Auth endpoints
            "/auth/login",
            "/auth/refresh",
            "/auth/captcha",
            // Captcha
            "/captcha/**",
            "/kaptcha/**",
            // Health check
            "/actuator/health",
            // API docs
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    private AuthConstant() {
    }
}
