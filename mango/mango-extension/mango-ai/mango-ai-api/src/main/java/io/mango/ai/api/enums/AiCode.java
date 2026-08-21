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
    CHAT_REQUEST_INVALID(95003, "对话参数不合法"),

    /** 用户标识不能为空。 */
    USER_REQUIRED(95004, "用户标识不能为空"),

    /** 对话请求超过调用频率限制。 */
    CHAT_RATE_LIMITED(95005, "AI 对话请求过于频繁"),

    /** AI 模型当前不可用。 */
    CHAT_MODEL_UNAVAILABLE(95006, "AI 模型当前不可用"),

    /** 会话上下文读写失败。 */
    CHAT_CONTEXT_UNAVAILABLE(95007, "AI 会话上下文不可用");

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
