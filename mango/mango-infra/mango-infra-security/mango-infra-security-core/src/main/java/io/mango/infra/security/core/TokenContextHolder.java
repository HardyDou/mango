package io.mango.infra.security.core;

/**
 * 使用 ThreadLocal 保存 token 上下文，用于当前线程内的 JWT token 传播。
 * <p>
 * 保存当前请求携带的 Authorization token，便于通过 Feign 调用继续传递给下游服务。
 *
 * @author Mango
 */
public class TokenContextHolder {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();

    private TokenContextHolder() {
    }

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
