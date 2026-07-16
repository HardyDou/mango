package io.mango.resource.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 资源注册中心业务错误码。
 */
@Getter
@AllArgsConstructor
public enum ResourceCode implements BizCode {

    /** 资源请求参数或声明不合法。 */
    RESOURCE_INVALID(400, "资源参数不合法"),

    /** 资源声明发生冲突。 */
    RESOURCE_CONFLICT(409, "资源声明冲突"),

    /** 资源或处理器不存在。 */
    RESOURCE_NOT_FOUND(404, "资源不存在"),

    /** 资源同步或远程执行失败。 */
    RESOURCE_SYNC_FAILED(500, "资源同步失败");

    private final int code;
    private final String message;
}
