package io.mango.i18n.api.enums;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum I18nCode implements BizCode {
    I18N_INVALID(400, "国际化参数不合法"),
    I18N_MESSAGE_NOT_FOUND(404, "I18n entry not found");

    private final int code;
    private final String message;
}
