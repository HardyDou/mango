package io.mango.area.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AreaCode implements BizCode {
    AREA_INVALID(400, "行政区划参数不合法"),
    AREA_NOT_FOUND(404, "Area not found"),
    AREA_PROTECTED(400, "标准行政区划不可修改编码或删除");

    private final int code;
    private final String message;
}
