package io.mango.org.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostCode implements BizCode {

    SUCCESS(200, "操作成功"),
    NOT_FOUND(404, "资源不存在"),
    VALIDATION_ERROR(400, "参数校验失败");

    private final int code;
    private final String message;
}
