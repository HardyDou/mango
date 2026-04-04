package io.mango.common.context;

/**
 * Token context holder using ThreadLocal for thread-safe JWT token propagation.
 * <p>
 * Stores the validated JWT token so it can be forwarded through Feign calls
 * to downstream services.
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
