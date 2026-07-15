package io.mango.access.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 登录上下文校验结果值对象。 */
@Schema(description = "登录上下文校验结果")
public final class AccessContextValidationResultVO {

    @Schema(description = "是否允许访问")
    private final boolean allowed;

    @Schema(description = "拒绝原因")
    private final String message;

    public AccessContextValidationResultVO(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    public boolean allowed() {
        return allowed;
    }

    public String message() {
        return message;
    }

    public static AccessContextValidationResultVO allow() {
        return new AccessContextValidationResultVO(true, null);
    }

    public static AccessContextValidationResultVO deny(String message) {
        return new AccessContextValidationResultVO(false, message);
    }
}
