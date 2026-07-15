package io.mango.access.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 边界访问处理结果值对象。 */
@Schema(description = "边界访问处理结果")
public final class AccessResultVO {

    @Schema(description = "访问处理状态") private final Status status;
    @Schema(description = "处理消息") private final String message;
    @Schema(description = "认证主体") private final AccessPrincipalVO principal;

    public AccessResultVO(Status status, String message, AccessPrincipalVO principal) {
        this.status = status;
        this.message = message;
        this.principal = principal;
    }

    public Status status() { return status; }
    public String message() { return message; }
    public AccessPrincipalVO principal() { return principal; }

    public boolean allowed() {
        return status == Status.ALLOW_ANONYMOUS
                || status == Status.ALLOW_AUTHENTICATED
                || status == Status.AUTH_DISABLED;
    }

    public static AccessResultVO allowAnonymous() {
        return new AccessResultVO(Status.ALLOW_ANONYMOUS, null, null);
    }

    public static AccessResultVO allowAuthenticated(AccessPrincipalVO principal) {
        return new AccessResultVO(Status.ALLOW_AUTHENTICATED, null, principal);
    }

    public static AccessResultVO disabled() {
        return new AccessResultVO(Status.AUTH_DISABLED, null, null);
    }

    public static AccessResultVO unauthorized(String message) {
        return new AccessResultVO(Status.UNAUTHORIZED, message, null);
    }

    public static AccessResultVO forbidden(String message) {
        return new AccessResultVO(Status.FORBIDDEN, message, null);
    }

    public static AccessResultVO unavailable(String message) {
        return new AccessResultVO(Status.SERVICE_UNAVAILABLE, message, null);
    }

    public enum Status {
        ALLOW_ANONYMOUS,
        ALLOW_AUTHENTICATED,
        AUTH_DISABLED,
        UNAUTHORIZED,
        FORBIDDEN,
        SERVICE_UNAVAILABLE
    }
}
