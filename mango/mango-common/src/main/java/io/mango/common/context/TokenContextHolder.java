package io.mango.common.context;

/**
 * Token 上下文持有者，基于 ThreadLocal 实现。
 * 用于在 Feign 调用链中传递 JWT Token。
 *
 * @author 杜天
 * @deprecated Use {@link io.mango.infra.security.core.TokenContextHolder} instead.
 *             This class remains here for backward compatibility.
 *             Will be removed in a future version.
 */
@Deprecated
public class TokenContextHolder {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    public static String getToken() {
        return TOKEN.get();
    }

    public static void setToken(String token) {
        TOKEN.set(token);
    }

    public static void clear() {
        TOKEN.remove();
    }
}
