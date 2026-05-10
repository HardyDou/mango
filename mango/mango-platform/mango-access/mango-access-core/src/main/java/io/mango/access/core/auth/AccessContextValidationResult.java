package io.mango.access.core.auth;

/**
 * 登录上下文校验结果。
 */
public record AccessContextValidationResult(
        boolean allowed,
        String message
) {

    public static AccessContextValidationResult allow() {
        return new AccessContextValidationResult(true, null);
    }

    public static AccessContextValidationResult deny(String message) {
        return new AccessContextValidationResult(false, message);
    }
}
