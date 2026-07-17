package io.mango.ai.api.enums;

import io.mango.common.result.BizCode;

/**
 * AI 模块业务错误码。
 */
public enum AiCode implements BizCode {

    /** 对话请求不能为空。 */
    CHAT_REQUEST_REQUIRED(95001, "对话请求不能为空"),

    /** 租户标识不能为空。 */
    TENANT_REQUIRED(95002, "租户标识不能为空"),

    /** 对话参数不合法。 */
    CHAT_REQUEST_INVALID(95003, "对话参数不合法");

    private final int code;
    private final String message;

    AiCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
