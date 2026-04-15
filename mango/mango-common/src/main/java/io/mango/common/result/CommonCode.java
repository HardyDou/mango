package io.mango.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用业务码枚举。
 *
 * @author Mango
 */
@Getter
@AllArgsConstructor
public enum CommonCode implements BizCode {

    /** 操作成功。 */
    SUCCESS(200, "操作成功"),

    /** 业务异常或参数校验失败。 */
    BAD_REQUEST(400, "参数校验失败"),

    /** 未登录或登录已过期。 */
    UNAUTHORIZED(401, "未登录"),

    /** 权限不足。 */
    FORBIDDEN(403, "权限不足"),

    /** 资源不存在。 */
    NOT_FOUND(404, "资源不存在"),

    /** 系统繁忙。 */
    SERVER_ERROR(500, "系统繁忙，请稍后再试");

    /** 业务状态码。 */
    private final int code;
    /** 默认消息。 */
    private final String message;
}
