package io.mango.authorization.access.core.auth;

/**
 * 网关访问处理结果。
 *
 * @author Mango
 */
public record AccessResult(
        Status status,
        String message,
        AccessPrincipal principal
) {

    public static AccessResult allowAnonymous() {
        return new AccessResult(Status.ALLOW_ANONYMOUS, null, null);
    }

    public static AccessResult allowAuthenticated(AccessPrincipal principal) {
        return new AccessResult(Status.ALLOW_AUTHENTICATED, null, principal);
    }

    public static AccessResult disabled() {
        return new AccessResult(Status.AUTH_DISABLED, null, null);
    }

    public static AccessResult unauthorized(String message) {
        return new AccessResult(Status.UNAUTHORIZED, message, null);
    }

    public static AccessResult forbidden(String message) {
        return new AccessResult(Status.FORBIDDEN, message, null);
    }

    public boolean allowed() {
        return status == Status.ALLOW_ANONYMOUS
                || status == Status.ALLOW_AUTHENTICATED
                || status == Status.AUTH_DISABLED;
    }

    public enum Status {
        ALLOW_ANONYMOUS,
        ALLOW_AUTHENTICATED,
        AUTH_DISABLED,
        UNAUTHORIZED,
        FORBIDDEN
    }
}
